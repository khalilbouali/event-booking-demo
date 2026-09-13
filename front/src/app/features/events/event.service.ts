import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

import {CreateEventRequest, EventSummary} from './event.model';

@Injectable({
  providedIn: 'root',
})
export class EventService {

  private readonly http = inject(HttpClient);

  findAll(): Observable<EventSummary[]> {
    return this.http.get<EventSummary[]>(
      '/api/events'
    );
  }

  findById(
    eventId: string
  ): Observable<EventSummary> {

    return this.http.get<EventSummary>(
      `/api/events/${eventId}`
    );
  }

  create(
    request: CreateEventRequest
  ): Observable<EventSummary> {

    return this.http.post<EventSummary>(
      '/api/events',
      request
    );
  }
}
