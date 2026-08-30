package com.mikelcrm.licenseservice.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSearchResult {
    private String id;
    private String slug;
    private String nombre;
    private String plan;
    private String estado;
}
