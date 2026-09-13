import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';

import { inject } from '@angular/core';
import { Router } from '@angular/router';

import {
  catchError,
  throwError
} from 'rxjs';

import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn =
  (request, next) => {

    const authService = inject(AuthService);
    const router = inject(Router);

    return next(request).pipe(

      catchError((error: HttpErrorResponse) => {

        if (
          error.status === 401 &&
          request.url.startsWith('/api/')
        ) {

          authService.clearAuthentication();

          if (router.url !== '/signed-out') {
            void router.navigateByUrl('/signed-out');
          }
        }

        return throwError(() => error);
      })
    );
  };
