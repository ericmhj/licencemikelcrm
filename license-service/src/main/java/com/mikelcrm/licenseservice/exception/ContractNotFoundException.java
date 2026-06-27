package com.mikelcrm.licenseservice.exception;

import java.util.UUID;

public class ContractNotFoundException extends RuntimeException {

    private final UUID contractId;

    public ContractNotFoundException(UUID contractId) {
        super("Contract not found: " + contractId);
        this.contractId = contractId;
    }

    public UUID getContractId() {
        return contractId;
    }
}
