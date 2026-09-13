import {Component, inject, signal} from '@angular/core';

import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';

import {MatDialogModule, MatDialogRef} from '@angular/material/dialog';

import {MatFormFieldModule} from '@angular/material/form-field';

import {MatInputModule} from '@angular/material/input';

import {MatButtonModule} from '@angular/material/button';

import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';

import {EventService} from '../event.service';

import {EventSummary} from '../event.model';

@Component({
  selector: 'app-create-event-dialog',

  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],

  templateUrl:
    './create-event-dialog.html',

  styleUrl:
    './create-event-dialog.scss'
})
export class CreateEventDialog {

  private static readonly MAX_SEATS = 25_740;

  private readonly eventService =
    inject(EventService);

  private readonly dialogRef =
    inject(
      MatDialogRef<
        CreateEventDialog,
        EventSummary
      >
    );

  private readonly formBuilder =
    inject(FormBuilder);

  readonly creating =
    signal(false);

  readonly error =
    signal<string | null>(null);

  readonly maxSeats =
    CreateEventDialog.MAX_SEATS;

  readonly form =
    this.formBuilder.nonNullable.group({

      name: [
        '',
        [
          Validators.required,
          Validators.maxLength(150)
        ]
      ],

      venue: [
        '',
        [
          Validators.required,
          Validators.maxLength(150)
        ]
      ],

      startsAt: [
        '',
        Validators.required
      ],

      seatCount: [
        1,
        [
          Validators.required,
          Validators.min(1),
          Validators.max(
            CreateEventDialog.MAX_SEATS
          )
        ]
      ],

      price: [
        1,
        [
          Validators.required,
          Validators.min(0.01)
        ]
      ]
    });

  cancel(): void {
    this.dialogRef.close();
  }

  create(): void {

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const {
      name,
      venue,
      startsAt,
      seatCount,
      price
    } = this.form.getRawValue();

    this.creating.set(true);
    this.error.set(null);

    this.eventService
      .create({
        name: name.trim(),
        venue: venue.trim(),
        startsAt:
          new Date(startsAt)
            .toISOString(),
        seatCount,
        price
      })
      .subscribe({

        next: event => {

          this.creating.set(false);

          this.dialogRef.close(
            event
          );
        },

        error: error => {

          this.creating.set(false);

          if (error.status === 400) {
            this.error.set(
              'Invalid event information.'
            );
            return;
          }

          if (error.status === 403) {
            this.error.set(
              'You are not authorized to create events.'
            );
            return;
          }

          this.error.set(
            'Unable to create event.'
          );
        }
      });
  }
}
