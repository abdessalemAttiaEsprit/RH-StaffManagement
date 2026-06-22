import { Routes } from '@angular/router';
//------ `mon_projet_rh` ------
import { PersonnelListComponent } from './mon_projet_rh/rh/personnel/personnel-list/personnel-list.component';
import { PersonnelFormComponent } from './mon_projet_rh/rh/personnel/personnel-form/personnel-form.component';
import { AbsencesListComponent } from './mon_projet_rh/rh/personnel/absence-list/absences-list.component';
import { PaymentListComponent } from './mon_projet_rh/rh/payment/payment-list.component';
import { CompanySettingsComponent } from './mon_projet_rh/adminRH/company-settings/company-settings.component';
import { JobPostingsComponent } from './mon_projet_rh/recruitment/job-postings/job-postings.component';
import { CareersComponent } from './mon_projet_rh/recruitment/careers/careers.component';
import { ApplyJobComponent } from './mon_projet_rh/recruitment/apply-job/apply-job.component';
import { AdminDashboardComponent } from './mon_projet_rh/adminRH/admin-dashboard/admin-dashboard.component';
import { ApplicationsListComponent } from './mon_projet_rh/recruitment/applications-list/applications-list.component';
import { PersonnelPortalComponent } from './mon_projet_rh/personnel/personnel-portal/personnel-portal.component';



export const routes: Routes = [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent},

      // Accessible USER (auth uniquement)
      { path: 'espace-personnel', component: PersonnelPortalComponent},
      { path: 'carrieres', component: CareersComponent },
      { path: 'candidature/:jobId', component: ApplyJobComponent },

      // Admin-only RH management
      { path: 'personnel', component: PersonnelListComponent, },
      { path: 'personnel/paiements', component: PaymentListComponent},
      { path: 'personnel/nouveau', component: PersonnelFormComponent },
      { path: 'personnel/absences', component: AbsencesListComponent },
      { path: 'personnel/modifier/:matricule', component: PersonnelFormComponent},

      { path: 'admin/parametres', component: CompanySettingsComponent },

      { path: 'recrutement/offres', component: JobPostingsComponent},
      { path: 'recrutement/candidatures/:jobId', component: ApplicationsListComponent},
      { path: '**', redirectTo: 'dashboard' }
    
];