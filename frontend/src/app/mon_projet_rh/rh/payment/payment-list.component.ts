import { Component, OnInit, ChangeDetectorRef, inject } from "@angular/core";
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { RouterLink, RouterLinkActive } from '@angular/router';
import { PaymentDTO } from "../../interface/payment.model";
import { PaymentService } from "../../../services/payment";
import { PaymentModelComponent } from "../payment/payment-model.component";
import { finalize } from 'rxjs/operators';

const monthMap: { [key: string]: number } = {
  'JANUARY': 1, 'FEBRUARY': 2, 'MARCH': 3, 'APRIL': 4,
  'MAY': 5, 'JUNE': 6, 'JULY': 7, 'AUGUST': 8,
  'SEPTEMBER': 9, 'OCTOBER': 10, 'NOVEMBER': 11, 'DECEMBER': 12
};

@Component({
  selector: 'app-payment-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, PaymentModelComponent],
  templateUrl: './payment-list.component.html',
  styleUrl: './payment-list.component.scss'
})
export class PaymentListComponent implements OnInit {
  private paymentService = inject(PaymentService);
  private cdr = inject(ChangeDetectorRef);

  payments: PaymentDTO[] = [];
  showAddModal = false;
  showEditModal = false;
  selectedPayment: any = null;
  loading = false;

  // Filtres
  filterMonth = 0; 
  filterYear = new Date().getFullYear();
  filterStatus = 'ALL';
  searchTerm = '';
  filterMinAmount = 0;
  filterMaxAmount = 0;
  
  availableYears: number[] = [];
  
  readonly TAUX_CNSS = 0.0918;
  readonly TAUX_IRPP_TRANCHE1 = 0.05;
  readonly TAUX_IRPP_TRANCHE2 = 0.10;
  readonly SEUIL_IRPP = 600;

  monthsMap = [
    { value: 0, label: 'Tous les mois' },
    { value: 1, label: 'Janvier' }, { value: 2, label: 'Février' },
    { value: 3, label: 'Mars' }, { value: 4, label: 'Avril' },
    { value: 5, label: 'Mai' }, { value: 6, label: 'Juin' },
    { value: 7, label: 'Juillet' }, { value: 8, label: 'Août' },
    { value: 9, label: 'Septembre' }, { value: 10, label: 'Octobre' },
    { value: 11, label: 'Novembre' }, { value: 12, label: 'Décembre' }
  ];

  ngOnInit(): void {
    this.initializeYears();
    this.loadPayments();
  }

  private initializeYears(): void {
    const currentYear = new Date().getFullYear();
    this.availableYears = [currentYear - 1, currentYear, currentYear + 1];
  }

  loadPayments(): void {
    this.loading = true;
    this.paymentService.getPayments()
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (data) => this.payments = [...data],
        error: (err) => console.error('Erreur chargement', err)
      });
  }

  // --- NOUVELLE MÉTHODE FIXÉE : generateAutoAll ---
  generateAutoAll(): void {
    const currentMonth = new Date().getMonth() + 1;
    const currentYear = new Date().getFullYear();

    if (confirm(`Générer les paiements pour tous les employés ?`)) {
      this.loading = true;
      this.paymentService.generateAllPayments(currentMonth, currentYear)
        .pipe(finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }))
        .subscribe({
          next: (results) => {
            alert(`${results.length} paiements générés.`);
            this.loadPayments(); 
          },
          error: (err) => alert("Erreur lors de la génération automatique.")
        });
    }
  }

  // --- NOUVELLE MÉTHODE FIXÉE : generatePDF ---
  generatePDF(p: PaymentDTO): void {
    if (!p.matricule) return;
    
    // Conversion sécurisée du mois
    const monthNum = typeof p.month === 'number' ? p.month : (monthMap[p.month.toUpperCase()] || parseInt(p.month));

    this.paymentService.downloadFichePaie(p.matricule, monthNum, p.year).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `Fiche_Paie_${p.matricule}_${p.month}_${p.year}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => alert("Impossible de générer le PDF.")
    });
  }

  onValidate(payment: PaymentDTO): void {
    if (!payment.matricule) return;
    const updated: PaymentDTO = { ...payment, status: 'PAID' };
    this.paymentService.updatePayment(payment.matricule, updated).subscribe({
      next: (res) => {
        this.payments = this.payments.map(p => p.matricule === res.matricule ? { ...res } : p);
        this.cdr.detectChanges();
      }
    });
  }

  onDelete(matricule: string): void {
    if (!matricule || !confirm(`Supprimer ce paiement ?`)) return;
    this.paymentService.deletePaymentByMatricule(matricule).subscribe({
      next: () => {
        this.payments = this.payments.filter(p => p.matricule !== matricule);
        this.cdr.detectChanges();
      }
    });
  }

  savePaymentUpdate(updatedData: PaymentDTO) {
    this.paymentService.updatePayment(updatedData.matricule, updatedData).subscribe({
      next: (res) => {
        this.payments = this.payments.map(p => p.matricule === res.matricule ? { ...res } : p);
        this.showEditModal = false;
        this.cdr.detectChanges();
      }
    });
  }

  // --- Helpers UI ---
  get filteredPayments(): PaymentDTO[] {
    return this.payments.filter(p => {
      if (this.filterMonth !== 0) {
        const m = typeof p.month === 'number' ? p.month : (monthMap[p.month.toUpperCase()] || parseInt(p.month));
        if (m !== this.filterMonth) return false;
      }
      if (p.year !== this.filterYear) return false;
      if (this.filterStatus !== 'ALL' && this.getEffectiveStatus(p) !== this.filterStatus) return false;
      if (this.searchTerm.trim()) {
        const s = this.searchTerm.toLowerCase();
        return p.fullPersonnelName?.toLowerCase().includes(s) || p.matricule?.toLowerCase().includes(s);
      }
      return true;
    });
  }

  getEffectiveStatus(p: any): string {
    if ((p.finalAmount ?? 0) < 0) return 'DISCIPLINE_COUNCIL';
    return p.status ?? 'PENDING';
  }

  isDisciplineCase(p: any): boolean { return this.getEffectiveStatus(p) === 'DISCIPLINE_COUNCIL'; }
  getStatusLabel(p: any): string {
    const s = this.getEffectiveStatus(p);
    return s === 'DISCIPLINE_COUNCIL' ? 'Conseil de discipline' : (s === 'PAID' ? 'Payé' : 'En attente');
  }
  hasPaymentIssue(p: PaymentDTO): boolean { return !p.rib || p.rib.length < 10; }

  // Stats
  get totalPaymentsCount() { return this.filteredPayments.length; }
  get totalPaidAmount() { return this.filteredPayments.filter(p => this.getEffectiveStatus(p) === 'PAID').reduce((acc, curr) => acc + (curr.finalAmount || 0), 0); }
  get totalPendingAmount() { return this.filteredPayments.filter(p => this.getEffectiveStatus(p) === 'PENDING').reduce((acc, curr) => acc + (curr.finalAmount || 0), 0); }
  get averageAmount() { return this.totalPaymentsCount > 0 ? (this.totalPaidAmount + this.totalPendingAmount) / this.totalPaymentsCount : 0; }

  // Filtres Events
  onMonthChange(event: any) { this.filterMonth = +event.target.value; this.cdr.detectChanges(); }
  onYearChange(event: any) { this.filterYear = +event.target.value; this.cdr.detectChanges(); }
  onStatusChange(event: any) { this.filterStatus = event.target.value; this.cdr.detectChanges(); }
  onFilterChange() { this.cdr.detectChanges(); }
  resetFilters() { this.filterMonth = 0; this.filterYear = new Date().getFullYear(); this.filterStatus = 'ALL'; this.searchTerm = ''; this.cdr.detectChanges(); }
  
  openEditModal(payment: any) { this.selectedPayment = { ...payment }; this.showEditModal = true; this.cdr.detectChanges(); }
  openGenerateModal() { this.showAddModal = true; this.cdr.detectChanges(); }
  onPaymentCreated(newPayment: PaymentDTO) { this.payments = [newPayment, ...this.payments]; this.showAddModal = false; this.cdr.detectChanges(); }
}