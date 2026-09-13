import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';

import {AuthService} from './auth.service';
import {map} from 'rxjs';

export const authGuard: CanActivateFn = () => {

  const authService =
    inject(AuthService);

  const router =
    inject(Router);

  if (authService.isInitialized()) {

    return authService.isAuthenticated()
      ? true
      : router.createUrlTree([
        '/signed-out'
      ]);
  }

  return authService.initialize().pipe(
    map(authenticated =>
      authenticated
        ? true
        : router.createUrlTree([
          '/signed-out'
        ])
    )
  );
};
