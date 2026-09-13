import {ReservationResponse} from '../../reservations/reservation.model';

export interface PaymentDialogData {
  eventName: string;
  price: number;
  reservation: ReservationResponse;
}
