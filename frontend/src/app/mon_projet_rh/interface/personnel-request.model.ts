export interface PersonnelRequest {
  id?: string;
  matricule: string;
  fullPersonnelName?: string;
  message: string;
  createdAt?: string;
  status?: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'PROCESSED' | string;
}
