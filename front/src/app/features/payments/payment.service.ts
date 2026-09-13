import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

import {PaymentResponse, ProcessPaymentRequest} from './payment.model';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {

  private readonly http = inject(HttpClient);

  process(
    request: ProcessPaymentRequest
  ): Observable<PaymentResponse> {

    return this.http.post<PaymentResponse>(
      '/api/payments',
      request
    );
  }

  findMine(): Observable<PaymentResponse[]> {
    return this.http.get<PaymentResponse[]>(
      '/api/payments'
    );
  }

  findById(
    paymentId: string
  ): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(
      `/api/payments/${paymentId}`
    );
  }
}
