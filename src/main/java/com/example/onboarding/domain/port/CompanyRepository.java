package com.example.onboarding.domain.port;

import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;

import java.util.Optional;

public interface CompanyRepository {

    void save(Company company);

    Optional<Company> findById(CompanyId id);
}
