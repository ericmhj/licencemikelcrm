package com.mikelcrm.licenseservice.domain.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolUsuarioId implements Serializable {

    private UUID usuarioId;
    private UUID tenant;
}
