package com.smartcity.security.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MobileOtpRepository {

    private static final Logger log = LoggerFactory.getLogger(MobileOtpRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public MobileOtpRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<MobileOtp> rowMapper = (rs, rowNum) -> {
        MobileOtp otp = new MobileOtp();
        String idStr = rs.getString("id");
        if (idStr != null) {
            otp.setId(UUID.fromString(idStr));
        }
        otp.setMobileNumber(rs.getString("mobile_number"));
        String userIdStr = rs.getString("user_id");
        if (userIdStr != null) {
            otp.setUserId(UUID.fromString(userIdStr));
        }
        otp.setOtpHash(rs.getString("otp_hash"));
        otp.setPurpose(rs.getString("purpose"));
        otp.setAttemptCount(rs.getInt("attempt_count"));
        otp.setMaxAttempts(rs.getInt("max_attempts"));
        Timestamp resend = rs.getTimestamp("resend_after");
        if (resend != null) otp.setResendAfter(resend.toInstant());
        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null) otp.setExpiresAt(expires.toInstant());
        otp.setUsed(rs.getBoolean("is_used"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) otp.setCreatedAt(created.toInstant());
        return otp;
    };

    @Transactional
    public MobileOtp save(MobileOtp otp) {
        if (otp.getId() == null) {
            otp.setId(UUID.randomUUID());
        }
        if (otp.getCreatedAt() == null) {
            otp.setCreatedAt(Instant.now());
        }

        String sql = """
            INSERT INTO mobile_otps (
                id, mobile_number, user_id, otp_hash, purpose, attempt_count,
                max_attempts, resend_after, expires_at, is_used, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                otp.getId(),
                otp.getMobileNumber(),
                otp.getUserId(),
                otp.getOtpHash(),
                otp.getPurpose(),
                otp.getAttemptCount(),
                otp.getMaxAttempts(),
                Timestamp.from(otp.getResendAfter()),
                Timestamp.from(otp.getExpiresAt()),
                otp.isUsed(),
                Timestamp.from(otp.getCreatedAt())
        );

        log.debug("Saved Mobile OTP record for {} ({})", otp.getMobileNumber(), otp.getPurpose());
        return otp;
    }

    @Transactional(readOnly = true)
    public Optional<MobileOtp> findLatestActiveOtp(String mobileNumber, String purpose) {
        String sql = """
            SELECT * FROM mobile_otps
            WHERE mobile_number = ? AND purpose = ? AND is_used = FALSE
            ORDER BY created_at DESC LIMIT 1
        """;
        List<MobileOtp> results = jdbcTemplate.query(sql, rowMapper, mobileNumber.trim(), purpose);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional(readOnly = true)
    public Optional<MobileOtp> findLatestByMobile(String mobileNumber, String purpose) {
        String sql = """
            SELECT * FROM mobile_otps
            WHERE mobile_number = ? AND purpose = ?
            ORDER BY created_at DESC LIMIT 1
        """;
        List<MobileOtp> results = jdbcTemplate.query(sql, rowMapper, mobileNumber.trim(), purpose);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public int incrementAttempts(UUID id) {
        String sql = "UPDATE mobile_otps SET attempt_count = attempt_count + 1 WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    @Transactional
    public int incrementAttemptsAtomic(UUID id) {
        String sql = "UPDATE mobile_otps SET attempt_count = attempt_count + 1 WHERE id = ? AND is_used = FALSE";
        return jdbcTemplate.update(sql, id);
    }

    @Transactional
    public int markUsed(UUID id) {
        String sql = "UPDATE mobile_otps SET is_used = TRUE WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    @Transactional
    public boolean markUsedAtomic(UUID id) {
        String sql = "UPDATE mobile_otps SET is_used = TRUE WHERE id = ? AND is_used = FALSE";
        return jdbcTemplate.update(sql, id) > 0;
    }

    @Transactional(readOnly = true)
    public int countRecentRequests(String mobileNumber, Instant since) {
        String sql = "SELECT COUNT(*) FROM mobile_otps WHERE mobile_number = ? AND created_at >= ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, mobileNumber.trim(), Timestamp.from(since));
        return count != null ? count : 0;
    }
}
