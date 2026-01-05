package com.lobosoft.enablebanking.domain;

import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankSessionRepository extends JpaRepository<@NonNull BankSession, @NonNull Long> {
    Optional<BankSession> findByState(String state);

    Optional<BankSession> findByUserIdAndAspspNameAndAspspCountry(
            String userId,
            String aspspName,
            String aspspCountry
    );

    void deleteByUserIdAndAspspNameAndAspspCountry(
            String userId,
            String aspspName,
            String aspspCountry
    );
}
