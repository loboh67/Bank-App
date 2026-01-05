package com.lobosoft.api.repository;

import com.lobosoft.api.model.BankSession;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface BankSessionRepository extends JpaRepository<@NonNull BankSession, @NonNull Long> {

    Optional<BankSession> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    java.util.List<BankSession> findByUserIdAndValidUntilGreaterThanEqual(
            String userId,
            OffsetDateTime validUntil
    );
}
