package com.mikelcrm.licenseservice.domain.entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogId implements Serializable {

    private UUID id;
    private LocalDateTime ocurridoEn;
}
