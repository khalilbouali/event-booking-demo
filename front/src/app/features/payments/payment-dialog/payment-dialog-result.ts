import { PaymentResponse } from '../payment.model';
import { ReservationResponse } from '../../reservations/reservation.model';

export interface PaymentDialogResult {
  reservation: ReservationResponse;
  payment: PaymentResponse;
}
