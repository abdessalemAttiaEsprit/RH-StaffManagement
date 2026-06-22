import { APP_INITIALIZER, ApplicationConfig, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http'; // Importé
import { routes } from './app.routes';
import { DataInitializationService } from './services/data-initialization.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(), // 👈 Ajouté ici pour que HttpClient fonctionnelle partout
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: () => {
        const initializer = inject(DataInitializationService);
        return () => initializer.initializeSmartParkJobs();
      }
    }
  ]
};