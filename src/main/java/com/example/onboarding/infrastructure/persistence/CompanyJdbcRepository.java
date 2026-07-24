package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.Company;
import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.CompanyStatus;
import com.example.onboarding.domain.model.ContactInfo;
import com.example.onboarding.domain.port.outbound.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CompanyJdbcRepository implements CompanyRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String INSERT = """
            INSERT INTO companies (company_id, company_name, admin_email, admin_first_name, admin_last_name, status, retry_count)
            VALUES (:companyId, :companyName, :adminEmail, :adminFirstName, :adminLastName, :status, :retryCount)
            """;

    private static final String UPDATE = """
            UPDATE companies
            SET status = :status, retry_count = :retryCount
            WHERE company_id = :companyId
            """;

    private static final String FIND_BY_ID = """
            SELECT company_id, company_name, admin_email, admin_first_name, admin_last_name, status, retry_count
            FROM companies
            WHERE company_id = :companyId
            """;

    @Override
    public void insert(Company company) {
        jdbc.update(INSERT, new MapSqlParameterSource()
                .addValue("companyId", company.getId().value())
                .addValue("companyName", company.getCompanyName())
                .addValue("adminEmail", company.getAdminContact().email())
                .addValue("adminFirstName", company.getAdminContact().firstName())
                .addValue("adminLastName", company.getAdminContact().lastName())
                .addValue("status", company.getStatus().name())
                .addValue("retryCount", company.getRetryCount()));
    }

    @Override
    public void update(Company company) {
        jdbc.update(UPDATE, new MapSqlParameterSource()
                .addValue("companyId", company.getId().value())
                .addValue("status", company.getStatus().name())
                .addValue("retryCount", company.getRetryCount()));
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        return jdbc.query(FIND_BY_ID,
                new MapSqlParameterSource("companyId", id.value()),
                COMPANY_ROW_MAPPER)
                .stream().findFirst();
    }

    private static final RowMapper<Company> COMPANY_ROW_MAPPER = (rs, rowNum) -> new Company(
            new CompanyId(rs.getObject("company_id", UUID.class)),
            rs.getString("company_name"),
            new ContactInfo(
                    rs.getString("admin_email"),
                    rs.getString("admin_first_name"),
                    rs.getString("admin_last_name")
            ),
            CompanyStatus.valueOf(rs.getString("status")),
            rs.getInt("retry_count")
    );
}
