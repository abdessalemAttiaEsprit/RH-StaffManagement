export interface CompanySettings {
  id?: string;
  companyName: string;
  address: string;
  matriculeFiscal: string;
  logoBase64?: string;
  signatureFileName?: string;
  phone: string;
}

export interface CompanyDTO {
  companyName: string;
  address: string;
  matriculeFiscal: string;
  logoBase64?: string;
  signatureFileName?: string;
  phone: string;
}
