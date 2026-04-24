import { APP_INITIALIZER, ApplicationConfig, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';
import { DataInitializationService } from './services/data-initialization.service';
import { AuthService } from './services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: () => {
        const initializer = inject(DataInitializationService);
        const auth = inject(AuthService);
        return () => {
          if (!auth.isLoggedIn()) return;
          initializer.initializeSmartParkJobs();
        };
      }
    }
  ]
};
