package com.lobosoft.api.repository;

import com.lobosoft.api.model.TransactionCategory;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionCategoryRepository extends JpaRepository<@NonNull TransactionCategory, @NonNull Long> {

    @Query("""
        SELECT tc
        FROM TransactionCategory tc
        JOIN FETCH tc.category c
        JOIN BankTransaction t ON t.id = tc.transactionId
        WHERE tc.transactionId = :transactionId
          AND t.userId = :userId
        ORDER BY tc.primaryCategory DESC, tc.confidence DESC
        """)
    List<TransactionCategory> findByTransactionIdAndUserId(@Param("transactionId") Long transactionId,
                                                           @Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM TransactionCategory tc WHERE tc.transactionId = :transactionId")
    int deleteByTransactionId(@Param("transactionId") Long transactionId);
}
