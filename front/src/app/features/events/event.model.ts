export interface EventSummary {
  id: string;
  name: string;
  venue: string;
  startsAt: string;
  seatCount: number;
  remainingSeatCount: number;
  price: number;
}

export interface CreateEventRequest {
  name: string;
  venue: string;
  startsAt: string;
  seatCount: number;
  price: number;
}
