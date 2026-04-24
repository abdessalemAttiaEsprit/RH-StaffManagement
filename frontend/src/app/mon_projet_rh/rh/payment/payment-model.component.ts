import { Component, EventEmitter, OnInit, Output, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core'; 
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaymentService } from '../../../services/payment';
import { PaymentDTO } from '../../interface/payment.model';
import { Personnel } from '../../interface/personnel.model';
import { PersonnelService } from '../../../services/personnel.service';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs/operators';

@Component({
  selector: 'app-payment-add',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './payment-model.component.html',
  styleUrl: './payment-model.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentModelComponent implements OnInit {
  // Injection moderne
  private fb = inject(FormBuilder);
  private paymentService = inject(PaymentService);
  private personnelService = inject(PersonnelService);
  private cdr = inject(ChangeDetectorRef);

  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<PaymentDTO>();
  
  paymentForm: FormGroup;
  personnelList: Personnel[] = []; 
  personnelFound: Personnel | null = null;
  loadingPersonnel = false;
  loadingList = false;
  calculatedBalance = 0;
  duplicatePaymentError = '';

  private readonly ABSENCE_QUOTA_DAYS = 2;
  private readonly WORKING_DAYS_PER_MONTH = 26;
  private computedPenalizedAbsenceDays = 0;

  months = [
    { name: 'Janvier', value: 1 }, { name: 'Février', value: 2 }, { name: 'Mars', value: 3 },
    { name: 'Avril', value: 4 }, { name: 'Mai', value: 5 }, { name: 'Juin', value: 6 },
    { name: 'Juillet', value: 7 }, { name: 'Août', value: 8 }, { name: 'Septembre', value: 9 },
    { name: 'Octobre', value: 10 }, { name: 'Novembre', value: 11 }, { name: 'Décembre', value: 12 }
  ];

  constructor() {
    this.paymentForm = this.fb.group({
      matricule: ['', Validators.required],
      cin: [{ value: '', disabled: true }, [Validators.required]],
      fullPersonnelName: [{ value: '', disabled: true }],
      salaireBase: [{ value: 0, disabled: true }],
      tauxHoraireSup: [{ value: 0, disabled: true }],
      rib: ['', [Validators.required, Validators.minLength(10)]], 
      cnssNumber: ['', [Validators.required]],
      month: [new Date().getMonth() + 1, [Validators.required]],
      year: [new Date().getFullYear(), Validators.required],
      totalAbsenceDays: [0, [Validators.min(0)]],
      deductionsAbsence: [{ value: 0, disabled: true }],
    });
  }

  ngOnInit() {
    this.loadPersonnelList();

    // Ecouteur pour la recherche manuelle (le champ texte du HTML)
    this.paymentForm.get('matricule')?.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(val => {
      if (val && val.trim().length > 2) {
        this.onSearchPersonnel();
      }
    });

    // Recalculer dès que le mois ou l'année change
    this.paymentForm.get('month')?.valueChanges.subscribe(() => this.onDateOrPersonnelChange());
    this.paymentForm.get('year')?.valueChanges.subscribe(() => this.onDateOrPersonnelChange());
  }

  loadPersonnelList() {
    this.loadingList = true;
    this.cdr.detectChanges();
    
    this.personnelService.getAll().pipe(
      finalize(() => {
        this.loadingList = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (data) => this.personnelList = data,
      error: (err) => console.error('Erreur liste personnel', err)
    });
  }

  onSelectChange() {
    // Appelé par le <select> du HTML
    this.onSearchPersonnel();
  }

  onSearchPersonnel() {
    const mat = this.paymentForm.get('matricule')?.value?.trim();
    if (!mat) return;

    this.loadingPersonnel = true;
    this.cdr.detectChanges();

    this.paymentService.getPersonnelByMatricule(mat).pipe(
      finalize(() => {
        this.loadingPersonnel = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (p: Personnel) => {
        this.personnelFound = p;
        this.paymentForm.patchValue({
          cin: p.cin,
          fullPersonnelName: `${p.prenom} ${p.nom}`,
          salaireBase: p.contrat?.salaireBase || 0,
          tauxHoraireSup: p.contrat?.tauxHoraireSup || 0,
          rib: p.rib || '',
          cnssNumber: p.cnssNumber || ''
        }, { emitEvent: false });
        
        this.onDateOrPersonnelChange();
      },
      error: () => {
        this.personnelFound = null;
        this.duplicatePaymentError = '';
      }
    });
  }

  private onDateOrPersonnelChange() {
    this.updateSolde();
    this.checkDuplicatePayment();
    this.cdr.detectChanges();
  }

  checkDuplicatePayment() {
    const month = this.paymentForm.get('month')?.value;
    const year = this.paymentForm.get('year')?.value;
    const mat = this.personnelFound?.matricule;
    
    if (!mat || !month || !year) return;

    this.paymentService.getPayments().subscribe({
      next: (payments) => {
        const existing = payments.find(p => 
          p.matricule === mat &&
          p.month.toString() === month.toString() &&
          p.year === year
        );

        this.duplicatePaymentError = existing 
          ? `⚠️ Un paiement existe déjà pour ${this.personnelFound?.nom} (${month}/${year})`
          : '';
        this.cdr.detectChanges();
      }
    });
  }

  updateSolde() {
    this.calculatedBalance = this.ABSENCE_QUOTA_DAYS;
    const month = +this.paymentForm.get('month')?.value;
    const year = +this.paymentForm.get('year')?.value;

    if (!this.personnelFound || !month || !year) return;

    const totalDays = this.computeAllAbsenceDaysForCurrentMonth(this.personnelFound, month, year);
    const nonJustifiedDays = this.computeNonJustifiedAbsenceDaysForCurrentMonth(this.personnelFound, month, year);
    
    this.computedPenalizedAbsenceDays = Math.max(0, nonJustifiedDays - this.ABSENCE_QUOTA_DAYS);

    this.paymentForm.patchValue({ totalAbsenceDays: totalDays }, { emitEvent: false });
    this.calculateDeductions();
  }

  calculateDeductions() {
    const salaire = this.paymentForm.get('salaireBase')?.value || 0;
    const dailyRate = (Number(salaire) || 0) / this.WORKING_DAYS_PER_MONTH;
    const penalite = dailyRate * this.computedPenalizedAbsenceDays;
    
    this.paymentForm.patchValue({ 
      deductionsAbsence: Number(penalite.toFixed(3)) 
    }, { emitEvent: false });
  }

  onSubmit() {
    if (this.duplicatePaymentError) {
      alert(this.duplicatePaymentError);
      return;
    }

    if (this.paymentForm.valid && this.personnelFound) {
      const { matricule, month, year, rib, cnssNumber } = this.paymentForm.getRawValue();
      
      this.paymentService.generate(matricule.trim(), month, year).subscribe({
        next: (res) => {
          // Mise à jour optionnelle des infos si elles ont changé dans le formulaire
          this.paymentService.updatePaymentInfo(matricule, { rib, cnssNumber }).subscribe();
          
          this.save.emit(res);
          this.onClose();
        },
        error: (err) => alert("Erreur : " + (err.error?.message || "Serveur indisponible"))
      });
    }
  }

  // --- Logique de calcul des dates (Inchangée mais fiabilisée) ---

  private parseLocalDate(dateValue: any): Date | null {
    if (!dateValue) return null;
    const d = new Date(dateValue);
    return isNaN(d.getTime()) ? null : new Date(d.getFullYear(), d.getMonth(), d.getDate());
  }

  private computeAllAbsenceDaysForCurrentMonth(p: Personnel, month: number, year: number): number {
    const startPeriod = new Date(year, month - 1, 1);
    const endPeriod = new Date(year, month, 0);

    return (p.absences || []).reduce((sum, a) => {
      const start = this.parseLocalDate(a.startDate);
      const end = this.parseLocalDate(a.endDate);
      if (!start || !end || end < startPeriod || start > endPeriod) return sum;
      
      const overlapStart = start < startPeriod ? startPeriod : start;
      const overlapEnd = end > endPeriod ? endPeriod : end;
      const diff = Math.floor((overlapEnd.getTime() - overlapStart.getTime()) / 86400000) + 1;
      return sum + diff;
    }, 0);
  }

  private computeNonJustifiedAbsenceDaysForCurrentMonth(p: Personnel, month: number, year: number): number {
    const nonJustified = (p.absences || []).filter(a => {
      const s = a.status?.toUpperCase();
      return s === 'REJECTED' || s === 'UNJUSTIFIED';
    });
    // On réutilise la même logique de calcul de jours sur la liste filtrée
    const tempPersonnel = { ...p, absences: nonJustified };
    return this.computeAllAbsenceDaysForCurrentMonth(tempPersonnel, month, year);
  }

  onClose() {
    this.close.emit();
  }
}