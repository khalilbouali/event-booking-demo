export type PaymentStatus =
  | 'PENDING'
  | 'SUCCEEDED'
  | 'FAILED';

export interface ProcessPaymentRequest {
  reservationId: string;
  amount: number;
  isValid: boolean;
}

export interface PaymentResponse {
  id: string;
  reservationId: string;
  amount: number;
  status: PaymentStatus;
  createdAt: string;

  eventId: string | null;
  seatId: string | null;
  eventName: string;
  eventVenue: string | null;
  eventStartsAt: string | null;
}
