export interface HoldSeatRequest {
  eventId: string;
}

export type ReservationStatus =
  | 'HELD'
  | 'CONFIRMED'
  | 'EXPIRED'
  | 'CANCELLED';

export interface ReservationResponse {
  id: string;
  eventId: string;
  seatId: string;
  customerId: string;
  status: ReservationStatus;
  createdAt: string;
  expiresAt: string;

  eventName: string;
  eventVenue: string;
  eventStartsAt: string;
}
