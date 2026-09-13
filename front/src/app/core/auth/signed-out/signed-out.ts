import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-signed-out',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './signed-out.html',
  styleUrls: ['./signed-out.scss'],
})
export class SignedOut {

  constructor(
    private readonly authService: AuthService
  ) {}

  login(): void {
    this.authService.login();
  }
}
