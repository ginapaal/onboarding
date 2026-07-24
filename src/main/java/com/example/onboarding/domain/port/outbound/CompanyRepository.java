package com.example.onboarding.domain.port.outbound;

import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;

import java.util.Optional;

public interface CompanyRepository {

    void insert(Company company);

    void update(Company company);

    Optional<Company> findById(CompanyId id);
}
