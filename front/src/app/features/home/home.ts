import {Component, inject} from '@angular/core';

import {RouterLink} from '@angular/router';

import {MatButtonModule} from '@angular/material/button';

import {MatDialog} from '@angular/material/dialog';

import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {AuthService} from '../../core/auth/auth.service';

import {CreateEventDialog} from '../events/create-event-dialog/create-event-dialog';
import {Observable, Subscription} from 'rxjs';

@Component({
  selector: 'app-home',

  imports: [
    RouterLink,
    MatButtonModule,
    MatSnackBarModule
  ],

  templateUrl:
    './home.html',

  styleUrl:
    './home.scss'
})
export class Home {

  readonly authService =
    inject(AuthService);

  private readonly dialog =
    inject(MatDialog);

  private readonly snackBar =
    inject(MatSnackBar);

  openCreateEvent(): void {

    const dialogRef =
      this.dialog.open(
        CreateEventDialog,
        {
          width: '560px',
          maxWidth: '95vw',
          disableClose: true
        }
      );

    dialogRef
      .afterClosed()
      .subscribe(event => {

        if (!event) {
          return;
        }

        this.snackBar.open(
          'Event created successfully. You can browse events now.',
          'Close',
          {
            duration: 5000
          }
        );
      });
  }

    logout(): void {
        this.authService.logout();
    }
}
