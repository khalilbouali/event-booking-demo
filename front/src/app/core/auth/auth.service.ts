import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {catchError, EMPTY, map, Observable, of, tap, throwError} from 'rxjs';

import { CurrentUser } from './current-user';
import {Router} from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly currentUser =
    signal<CurrentUser | null>(null);

  readonly user =
    this.currentUser.asReadonly();

  private readonly initialized =
    signal(false);

  isInitialized(): boolean {
    return this.initialized();
  }

  readonly isAuthenticated =
    computed(() => this.currentUser() !== null);

  initialize(): Observable<boolean> {

    return this.http
      .get<CurrentUser>('/api/me')
      .pipe(
        tap(user => {
          this.currentUser.set(user);
          this.initialized.set(true);
        }),

        map(() => true),

        catchError((error: HttpErrorResponse) => {

          this.initialized.set(true);

          if (error.status === 401) {
            this.currentUser.set(null);
            return of(false);
          }

          return throwError(() => error);
        })
      );
  }

  login(): void {

    window.location.assign(
      '/oauth2/authorization/cloud-bff'
    );
  }

  logout(): void {

    const csrfToken = this.getCookie('XSRF-TOKEN');

    if (!csrfToken) {
      console.error('CSRF token not found');
      return;
    }

    const form = document.createElement('form');

    form.method = 'POST';
    form.action = '/logout';

    const csrfInput = document.createElement('input');

    csrfInput.type = 'hidden';
    csrfInput.name = '_csrf';
    csrfInput.value = csrfToken;

    form.appendChild(csrfInput);

    document.body.appendChild(form);

    form.submit();
  }

  private getCookie(name: string): string | null {

    const prefix = `${name}=`;

    const cookie = document.cookie
      .split(';')
      .map(value => value.trim())
      .find(value => value.startsWith(prefix));

    return cookie
      ? decodeURIComponent(cookie.substring(prefix.length))
      : null;
  }

  clearAuthentication(): void {
    this.currentUser.set(null);
  }
}
