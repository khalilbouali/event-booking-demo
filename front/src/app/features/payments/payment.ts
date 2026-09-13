import {Component, inject, signal} from '@angular/core';

import {DatePipe} from '@angular/common';

import {MatCardModule} from '@angular/material/card';

import {PaymentResponse} from './payment.model';

import {PaymentService} from './payment.service';

@Component({
  selector: 'app-my-payments',
  imports: [
    DatePipe,
    MatCardModule
  ],
  templateUrl: './payment.html',
  styleUrl: './payment.scss'
})
export class PaymentComponent {

  private readonly paymentService =
    inject(PaymentService);

  readonly payments =
    signal<PaymentResponse[]>([]);

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

    this.paymentService
      .findMine()
      .subscribe({

        next: payments => {
          this.payments.set(payments);
          this.loading.set(false);
        },

        error: () => {
          this.error.set(
            'Unable to load your payments.'
          );

          this.loading.set(false);
        }
      });
  }
}
