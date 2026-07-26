package com.example.onboarding.infrastructure.persistence;

import com.example.onboarding.domain.model.CompanyId;
import com.example.onboarding.domain.model.OnboardingSession;
import com.example.onboarding.domain.model.OnboardingSessionId;
import com.example.onboarding.domain.model.PaymentIntentId;
import com.example.onboarding.domain.port.outbound.OnboardingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OnboardingSessionJdbcRepository implements OnboardingSessionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String INSERT = """
            INSERT INTO onboarding_sessions (session_id, company_id, payment_intent_id)
            VALUES (:sessionId, (SELECT id FROM companies WHERE company_id = :companyId), :paymentIntentId)
            """;

    private static final String UPDATE = """
            UPDATE onboarding_sessions
            SET payment_intent_id = :paymentIntentId
            WHERE session_id = :sessionId
            """;

    private static final String FIND_BY_ID = """
            SELECT s.session_id, c.company_id, s.payment_intent_id
            FROM onboarding_sessions s
            JOIN companies c ON s.company_id = c.id
            WHERE s.session_id = :sessionId
            """;

    private static final String FIND_BY_PAYMENT_INTENT_ID = """
            SELECT s.session_id, c.company_id, s.payment_intent_id
            FROM onboarding_sessions s
            JOIN companies c ON s.company_id = c.id
            WHERE s.payment_intent_id = :paymentIntentId
            """;

    @Override
    public void insert(OnboardingSession session) {
        jdbc.update(INSERT, new MapSqlParameterSource()
                .addValue("sessionId", session.getId().value())
                .addValue("companyId", session.getCompanyId().value())
                .addValue("paymentIntentId", session.getPaymentIntentId() != null ? session.getPaymentIntentId().value() : null));
    }

    @Override
    public void update(OnboardingSession session) {
        jdbc.update(UPDATE, new MapSqlParameterSource()
                .addValue("sessionId", session.getId().value())
                .addValue("paymentIntentId", session.getPaymentIntentId() != null ? session.getPaymentIntentId().value() : null));
    }

    @Override
    public Optional<OnboardingSession> findById(OnboardingSessionId id) {
        return jdbc.query(FIND_BY_ID,
                new MapSqlParameterSource("sessionId", id.value()),
                SESSION_ROW_MAPPER)
                .stream().findFirst();
    }

    @Override
    public Optional<OnboardingSession> findByPaymentIntentId(PaymentIntentId paymentIntentId) {
        return jdbc.query(FIND_BY_PAYMENT_INTENT_ID,
                new MapSqlParameterSource("paymentIntentId", paymentIntentId.value()),
                SESSION_ROW_MAPPER)
                .stream().findFirst();
    }

    private static final RowMapper<OnboardingSession> SESSION_ROW_MAPPER = (rs, rowNum) -> {
        String paymentIntentIdValue = rs.getString("payment_intent_id");
        PaymentIntentId paymentIntentId = paymentIntentIdValue != null
                ? new PaymentIntentId(paymentIntentIdValue)
                : null;

        return new OnboardingSession(
                new OnboardingSessionId(rs.getObject("session_id", UUID.class)),
                new CompanyId(rs.getObject("company_id", UUID.class)),
                paymentIntentId
        );
    };
}
