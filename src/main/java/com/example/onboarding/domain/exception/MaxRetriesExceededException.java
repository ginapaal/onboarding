package com.example.onboarding.domain.exception;

import com.example.onboarding.domain.model.CompanyId;
import lombok.Getter;

@Getter
public class MaxRetriesExceededException extends RuntimeException {

    private final CompanyId companyId;
    private final int retryCount;

    public MaxRetriesExceededException(CompanyId companyId, int retryCount) {
        super("Company " + companyId.value() + " exceeded max payment retries (" + retryCount + ")");
        this.companyId = companyId;
        this.retryCount = retryCount;
    }

}
