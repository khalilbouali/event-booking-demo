import {Component, inject, signal} from '@angular/core';

import {FormsModule} from '@angular/forms';

import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from '@angular/material/dialog';

import {MatButtonModule} from '@angular/material/button';

import {MatCheckboxModule} from '@angular/material/checkbox';

import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';

import {finalize} from 'rxjs';

import {PaymentService} from '../payment.service';

import {PaymentDialogData} from './payment-dialog-data';

import {PaymentDialogResult} from './payment-dialog-result';

@Component({
  selector: 'app-payment-dialog',

  imports: [
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule
  ],

  templateUrl: './payment-dialog.html',
  styleUrl: './payment-dialog.scss'
})
export class PaymentDialog {

  readonly data =
    inject<PaymentDialogData>(
      MAT_DIALOG_DATA
    );

  private readonly dialogRef =
    inject<
      MatDialogRef<
        PaymentDialog,
        PaymentDialogResult
      >
    >(MatDialogRef);

  private readonly paymentService =
    inject(PaymentService);

  readonly processing =
    signal(false);

  readonly error =
    signal<string | null>(null);

  isValid = true;

  cancel(): void {
    this.dialogRef.close();
  }

  pay(): void {

    if (this.processing()) {
      return;
    }

    this.processing.set(true);
    this.error.set(null);

    this.paymentService
      .process({
        reservationId: this.data.reservation.id,
        amount: this.data.price,
        isValid: this.isValid
      })
      .pipe(
        finalize(() => {
          this.processing.set(false);
        })
      )
      .subscribe({

        next: payment => {

          this.dialogRef.close({
            reservation: this.data.reservation,
            payment
          });
        },

        error: () => {

          this.error.set(
            'Unable to process your payment.'
          );
        }
      });
  }
}
