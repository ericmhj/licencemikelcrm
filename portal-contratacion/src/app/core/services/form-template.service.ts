import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FormTemplate {
  id: string;
  formType: string;
  name: string;
  description: string | null;
  htmlContent: string;
  fieldsMetadata: FieldsMetadata;
  currentVersion: number;
  isActive: boolean;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FieldsMetadata {
  sections: Array<{
    sectionName: string;
    fields: string[];
  }>;
}

export interface CreateFormTemplateRequest {
  form_type: string;
  name: string;
  description?: string;
  html_content: string;
}

export interface UpdateFormTemplateRequest {
  name?: string;
  description?: string;
  html_content?: string;
}

@Injectable({ providedIn: 'root' })
export class FormTemplateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.smtApiUrl}/api/form-templates`;

  getAll(): Observable<FormTemplate[]> {
    return this.http.get<FormTemplate[]>(this.baseUrl);
  }

  getById(id: string): Observable<FormTemplate> {
    return this.http.get<FormTemplate>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateFormTemplateRequest): Observable<FormTemplate> {
    return this.http.post<FormTemplate>(this.baseUrl, data);
  }

  update(id: string, data: UpdateFormTemplateRequest): Observable<FormTemplate> {
    return this.http.put<FormTemplate>(`${this.baseUrl}/${id}`, data);
  }

  toggle(id: string): Observable<FormTemplate> {
    return this.http.patch<FormTemplate>(`${this.baseUrl}/${id}/toggle`, {});
  }
}
