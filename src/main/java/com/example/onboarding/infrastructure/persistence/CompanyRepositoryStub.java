package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.port.CompanyRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CompanyRepositoryStub implements CompanyRepository {

    @Override
    public void save(Company company) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
