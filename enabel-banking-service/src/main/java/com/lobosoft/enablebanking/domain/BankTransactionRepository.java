package com.lobosoft.enablebanking.domain;

import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankTransactionRepository extends JpaRepository<@NonNull BankTransaction, @NonNull Long> {

    List<BankTransaction> findByUserIdAndBankAccountIdOrderByIdDesc(String userId, Long bankAccountId);

    List<BankTransaction> findByUserIdOrderByIdDesc(String userId);
}
