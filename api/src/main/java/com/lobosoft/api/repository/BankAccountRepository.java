package com.lobosoft.api.repository;

import com.lobosoft.api.model.BankAccount;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<@NonNull BankAccount, @NonNull Long> {

    List<BankAccount> findByUserId(String userId);

    boolean existsByIdAndUserId(Long id, String userId);

    // List<BankAccount> findByUserIdAndStatus(UUID userId, BankAccountStatus status);

    @Modifying
    @Query("""
        UPDATE BankAccount b
           SET b.ebContinuationKey = NULL
         WHERE b.id = :accountId
           AND b.userId = :userId
        """)
    int clearContinuationKey(@Param("accountId") Long accountId,
                             @Param("userId") String userId);

    @Query("""
        SELECT b.ebContinuationKey
          FROM BankAccount b
         WHERE b.id = :accountId
           AND b.userId = :userId
        """)
    String findContinuationKey(@Param("accountId") Long accountId,
                               @Param("userId") String userId);
}
