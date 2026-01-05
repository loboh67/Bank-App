package com.lobosoft.api.repository;

import com.lobosoft.api.model.BankTransaction;
import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<@NonNull BankTransaction, @NonNull Long> {

    @EntityGraph(attributePaths = "merchant")
    List<BankTransaction> findByUserIdAndBankAccountIdOrderByBookingDateDescIdDesc(String userId, Long bankAccountId);

    @EntityGraph(attributePaths = "merchant")
    Page<BankTransaction> findByUserIdAndBankAccountIdOrderByBookingDateDescIdDesc(String userId, Long bankAccountId, Pageable pageable);

    @EntityGraph(attributePaths = "merchant")
    List<BankTransaction> findByUserIdOrderByBookingDateDescIdDesc(String userId);

    boolean existsByIdAndUserId(Long id, String userId);

    @EntityGraph(attributePaths = "merchant")
    Optional<BankTransaction> findByIdAndUserIdAndBankAccountId(Long id, String userId, Long bankAccountId);

    @EntityGraph(attributePaths = "merchant")
    @Query("""
        SELECT DISTINCT t
        FROM BankTransaction t
        JOIN TransactionCategory tc ON tc.transactionId = t.id
        WHERE t.userId = :userId
          AND tc.categoryId = :categoryId
          AND (:fromDate IS NULL OR t.bookingDate >= :fromDate)
          AND (:toDate IS NULL OR t.bookingDate <= :toDate)
        ORDER BY t.bookingDate DESC, t.id DESC
        """)
    List<BankTransaction> findByUserIdAndCategoryIdBetweenDates(@Param("userId") String userId,
                                                                @Param("categoryId") Long categoryId,
                                                                @Param("fromDate") LocalDate fromDate,
                                                                @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN t.amount < 0 THEN t.amount
                WHEN UPPER(t.direction) IN ('OUT', 'OUTGOING', 'DEBIT') THEN -t.amount
                ELSE t.amount
            END
        ), 0)
        FROM BankTransaction t
        JOIN TransactionCategory tc ON tc.transactionId = t.id
        WHERE t.userId = :userId
          AND tc.categoryId = :categoryId
        """)
    BigDecimal calculateTotalSpentForCategory(@Param("userId") String userId,
                                              @Param("categoryId") Long categoryId);

    @EntityGraph(attributePaths = "merchant")
    @Query("""
        SELECT DISTINCT t
        FROM BankTransaction t
        WHERE t.userId = :userId
          AND t.merchant.id = :merchantId
          AND (:fromDate IS NULL OR t.bookingDate >= :fromDate)
          AND (:toDate IS NULL OR t.bookingDate <= :toDate)
        ORDER BY t.bookingDate DESC, t.id DESC
        """)
    List<BankTransaction> findByUserIdAndMerchantIdBetweenDates(@Param("userId") String userId,
                                                                @Param("merchantId") Long merchantId,
                                                                @Param("fromDate") LocalDate fromDate,
                                                                @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN t.amount < 0 THEN t.amount
                WHEN UPPER(t.direction) IN ('OUT', 'OUTGOING', 'DEBIT') THEN -t.amount
                ELSE t.amount
            END
        ), 0)
        FROM BankTransaction t
        WHERE t.userId = :userId
          AND t.merchant.id = :merchantId
        """)
    BigDecimal calculateTotalSpentForMerchant(@Param("userId") String userId,
                                              @Param("merchantId") Long merchantId);

    @EntityGraph(attributePaths = "merchant")
    @Query("""
        SELECT DISTINCT t
        FROM BankTransaction t
        WHERE t.userId = :userId
          AND t.descriptionRaw = :descriptionRaw
          AND (:fromDate IS NULL OR t.bookingDate >= :fromDate)
          AND (:toDate IS NULL OR t.bookingDate <= :toDate)
        ORDER BY t.bookingDate DESC, t.id DESC
        """)
    List<BankTransaction> findByUserIdAndDescriptionRawBetweenDates(@Param("userId") String userId,
                                                                    @Param("descriptionRaw") String descriptionRaw,
                                                                    @Param("fromDate") LocalDate fromDate,
                                                                    @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN t.amount < 0 THEN t.amount
                WHEN UPPER(t.direction) IN ('OUT', 'OUTGOING', 'DEBIT') THEN -t.amount
                ELSE t.amount
            END
        ), 0)
        FROM BankTransaction t
        WHERE t.userId = :userId
          AND t.descriptionRaw = :descriptionRaw
        """)
    BigDecimal calculateTotalSpentForDescription(@Param("userId") String userId,
                                                 @Param("descriptionRaw") String descriptionRaw);

    @Modifying
    @Query("""
        DELETE FROM BankTransaction t
        WHERE t.bankAccountId = :accountId
          AND t.userId = :userId
        """)
    int deleteByAccountIdAndUserId(@Param("accountId") Long accountId,
                                   @Param("userId") String userId);

    @Modifying
    @Query("""
        UPDATE BankTransaction t
        SET t.descriptionDisplay = :descriptionDisplay
        WHERE t.userId = :userId
          AND t.descriptionRaw = :descriptionRaw
        """)
    int updateDescriptionDisplayForDescriptionRaw(@Param("userId") String userId,
                                                  @Param("descriptionRaw") String descriptionRaw,
                                                  @Param("descriptionDisplay") String descriptionDisplay);
}
