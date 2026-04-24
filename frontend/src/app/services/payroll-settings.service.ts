import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PayrollSettings } from '../mon_projet_rh/interface/payroll-settings.model';

@Injectable({ providedIn: 'root' })
export class PayrollSettingsService {
  private readonly API_URL = 'http://localhost:8081/api/payroll/settings';

  constructor(private http: HttpClient) {}

  getSettings(): Observable<PayrollSettings> {
    return this.http.get<PayrollSettings>(this.API_URL);
  }
}
