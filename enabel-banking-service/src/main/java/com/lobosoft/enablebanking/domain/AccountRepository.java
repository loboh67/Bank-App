package com.lobosoft.enablebanking.domain;

import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<@NonNull Account, @NonNull Long> {
    Optional<Account> findByUserIdAndProviderAccountId(String userId, String providerAccountId);

    Optional<Account> findByUserIdAndIban(String userId, String iban);

    Optional<Account> findByIdAndUserId(Long id, String userId);
}
