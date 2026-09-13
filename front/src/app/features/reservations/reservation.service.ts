import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

import {HoldSeatRequest, ReservationResponse} from './reservation.model';

@Injectable({
  providedIn: 'root',
})
export class ReservationService {

  private readonly http = inject(HttpClient);

  hold(
    request: HoldSeatRequest
  ): Observable<ReservationResponse> {

    return this.http.post<ReservationResponse>(
      '/api/reservations',
      request
    );
  }

  findMine(): Observable<ReservationResponse[]> {
    return this.http.get<ReservationResponse[]>(
      '/api/reservations'
    );
  }
}
