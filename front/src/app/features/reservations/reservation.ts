import {Component, inject, signal} from '@angular/core';

import {DatePipe} from '@angular/common';

import {MatCardModule} from '@angular/material/card';

import {ReservationResponse} from './reservation.model';

import {ReservationService} from './reservation.service';

@Component({
  selector: 'app-reservation',
  imports: [
    DatePipe,
    MatCardModule
  ],
  templateUrl: './reservation.html',
  styleUrl: './reservation.scss'
})
export class ReservationComponent {

  private readonly reservationService =
    inject(ReservationService);

  readonly reservations =
    signal<ReservationResponse[]>([]);

  readonly loading =
    signal(true);

  readonly error =
    signal<string | null>(null);

  constructor() {
    this.load();
  }

  private load(): void {

    this.loading.set(true);
    this.error.set(null);

    this.reservationService
      .findMine()
      .subscribe({

        next: reservations => {
          this.reservations.set(reservations);
          this.loading.set(false);
        },

        error: () => {
          this.error.set(
            'Unable to load your reservations.'
          );

          this.loading.set(false);
        }
      });
  }
}
