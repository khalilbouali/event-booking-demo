import {Component, inject, signal} from '@angular/core';

import {DatePipe} from '@angular/common';

import {MatButtonModule} from '@angular/material/button';

import {MatCardModule} from '@angular/material/card';

import {MatDialog} from '@angular/material/dialog';

import {EventService} from './event.service';

import {EventSummary} from './event.model';

import {PaymentDialog} from '../payments/payment-dialog/payment-dialog';

import {PaymentDialogData} from '../payments/payment-dialog/payment-dialog-data';
import {PaymentDialogResult} from '../payments/payment-dialog/payment-dialog-result';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {timer} from 'rxjs';
import {ReservationService} from '../reservations/reservation.service';

@Component({
  selector: 'app-events',

  imports: [
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatSnackBarModule,
  ],

  templateUrl:
    './event.html',

  styleUrl:
    './event.scss'
})
export class EventComponent {

  private readonly eventService =
    inject(EventService);

  private readonly reservationService =
    inject(ReservationService);

  private readonly dialog =
    inject(MatDialog);

  private readonly snackBar =
    inject(MatSnackBar);

  readonly events =
    signal<EventSummary[]>([]);

  readonly loading =
    signal(false);

  readonly error =
    signal<string | null>(null);

  readonly reservationInProgress =
    signal(false);

  constructor() {
    this.loadEvents();
  }

  private loadEvents(): void {

    this.loading.set(true);
    this.error.set(null);

    this.eventService
      .findAll()
      .subscribe({

        next: events => {
          this.events.set(events);
          this.loading.set(false);
        },

        error: () => {
          this.error.set(
            'Unable to load events.'
          );

          this.loading.set(false);
        }
      });
  }

  openPaymentDialog(event: EventSummary): void {

    if (
      event.remainingSeatCount === 0 ||
      this.reservationInProgress()
    ) {
      return;
    }

    this.reservationInProgress.set(true);

    this.reservationService
      .hold({
        eventId: event.id
      })
      .subscribe({

        next: reservation => {

          this.reservationInProgress.set(false);

          const dialogRef =
            this.dialog.open<
              PaymentDialog,
              PaymentDialogData,
              PaymentDialogResult
            >(
              PaymentDialog,
              {
                width: '480px',

                data: {
                  eventName: event.name,
                  price: event.price,
                  reservation
                },

                disableClose: true
              }
            );

          dialogRef
            .afterClosed()
            .subscribe(result => {

              if (!result) {
                return;
              }

              if (result.payment.status === 'SUCCEEDED') {

                this.snackBar.open(
                  `Payment successful. Seat ${result.reservation.seatId} was allocated to you.`,
                  'Close',
                  {
                    duration: 5000
                  }
                );

              } else {

                this.snackBar.open(
                  'Payment failed. Your reservation will be released.',
                  'Close',
                  {
                    duration: 5000
                  }
                );
              }

              timer(1000)
                .subscribe(() => this.loadEvents());
            });
        },

        error: error => {

          this.reservationInProgress.set(false);

          if (error.status === 409) {

            this.snackBar.open(
              'No seats are currently available for this event.',
              'Close',
              {
                duration: 5000
              }
            );

          } else {

            this.snackBar.open(
              'Unable to reserve a seat.',
              'Close',
              {
                duration: 5000
              }
            );
          }
        }
      });
  }
}
