import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PersonnelRequest } from '../mon_projet_rh/interface/personnel-request.model';

@Injectable({ providedIn: 'root' })
export class PersonnelRequestsService {
  private readonly API_URL = 'http://localhost:8081/api/personnel-requests';

  constructor(private http: HttpClient) {}

  create(matricule: string, message: string): Observable<PersonnelRequest> {
    return this.http.post<PersonnelRequest>(`${this.API_URL}/matricule/${matricule}`, { message });
  }

  listByMatricule(matricule: string): Observable<PersonnelRequest[]> {
    return this.http.get<PersonnelRequest[]>(`${this.API_URL}/matricule/${matricule}`);
  }

  listAll(): Observable<PersonnelRequest[]> {
    return this.http.get<PersonnelRequest[]>(this.API_URL);
  }

  updateStatus(id: string, status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | string): Observable<PersonnelRequest> {
    return this.http.patch<PersonnelRequest>(`${this.API_URL}/${id}/status`, { status });
  }

  updateMessage(id: string, matricule: string, message: string): Observable<PersonnelRequest> {
    return this.http.put<PersonnelRequest>(`${this.API_URL}/${id}`, { matricule, message });
  }

  delete(id: string, matricule?: string): Observable<void> {
    const url = `${this.API_URL}/${id}`;
    if (matricule) {
      return this.http.request<void>('delete', url, { body: { matricule } });
    }
    return this.http.delete<void>(url);
  }
}
