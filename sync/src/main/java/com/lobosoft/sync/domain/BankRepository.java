package com.lobosoft.sync.domain;

import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BankRepository extends JpaRepository<@NonNull BankSession, @NonNull Long> {
    @Query("""
        SELECT s FROM BankSession s
        WHERE (s.validUntil IS NULL OR s.validUntil > :now)
          AND s.status = 'AUTHORIZED'
    """)
    List<BankSession> findActiveSessions(@Param("now") Instant now);
}