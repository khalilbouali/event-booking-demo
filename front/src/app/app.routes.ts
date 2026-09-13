import {Routes} from '@angular/router';
import {authGuard} from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'signed-out',
    loadComponent: () =>
      import('./core/auth/signed-out/signed-out')
        .then(m => m.SignedOut)
  },

  // protected routes
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/home/home')
            .then(m => m.Home)
      },

      {
        path: 'events',
        loadComponent: () =>
          import('./features/events/event')
            .then(m => m.EventComponent)
      },

      {
        path: 'reservations',
        loadComponent: () =>
          import('./features/reservations/reservation')
            .then(m => m.ReservationComponent)
      },

      {
        path: 'payments',
        loadComponent: () =>
          import('./features/payments/payment')
            .then(m => m.PaymentComponent)
      }
    ]
  },

  {
    path: '**',
    redirectTo: 'signed-out'
  }
];
