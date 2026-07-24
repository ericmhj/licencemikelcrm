import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  FuncionRoles,
  CreateFuncionRolesRequest,
  UpdateFuncionRolesRequest,
} from '../models/funcion-roles.model';

@Injectable({ providedIn: 'root' })
export class FuncionRolesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl: string;

  constructor() {
    const apiUrl = environment?.apiUrl;
    // In production with nginx proxy, apiUrl can be empty string (relative URLs)
    // Only fail if explicitly undefined or null
    if (apiUrl === undefined || apiUrl === null) {
      throw new Error(
        'FuncionRolesService: environment.apiUrl is missing or invalid. Cannot initialize service.'
      );
    }
    this.baseUrl = apiUrl ? `${apiUrl}/api/v1/funcion-roles` : '/api/v1/funcion-roles';
  }

  getAll(): Observable<FuncionRoles[]> {
    return this.http
      .get<FuncionRoles[]>(this.baseUrl)
      .pipe(catchError(this.handleError));
  }

  getByCode(codigo: string): Observable<FuncionRoles> {
    return this.http
      .get<FuncionRoles>(`${this.baseUrl}/${codigo}`)
      .pipe(catchError(this.handleError));
  }

  create(request: CreateFuncionRolesRequest): Observable<FuncionRoles> {
    return this.http
      .post<FuncionRoles>(this.baseUrl, request)
      .pipe(catchError(this.handleError));
  }

  update(
    codigo: string,
    request: UpdateFuncionRolesRequest
  ): Observable<FuncionRoles> {
    return this.http
      .put<FuncionRoles>(`${this.baseUrl}/${codigo}`, request)
      .pipe(catchError(this.handleError));
  }

  delete(codigo: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${codigo}`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let message: string;

    if (error.status === 0) {
      message = 'Error de conectividad: no se pudo contactar con el servidor.';
    } else if (error.error?.message) {
      message = error.error.message;
    } else {
      switch (error.status) {
        case 400:
          message = 'Solicitud inválida. Verifica los datos enviados.';
          break;
        case 404:
          message = 'Recurso no encontrado.';
          break;
        case 409:
          message = 'Conflicto: el recurso ya existe o hay un duplicado.';
          break;
        default:
          message = `Error inesperado (${error.status}): ${error.statusText}`;
      }
    }

    return throwError(() => ({ status: error.status, message, error: error.error }));
  }
}
