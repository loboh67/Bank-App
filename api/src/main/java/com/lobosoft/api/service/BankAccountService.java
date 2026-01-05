package com.lobosoft.api.service;

import com.lobosoft.api.dto.BankAccountResponse;
import com.lobosoft.api.repository.BankAccountRepository;
import com.lobosoft.api.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public List<BankAccountResponse> listUserAccounts(String userId) {
        return bankAccountRepository.findByUserId(userId).stream()
                .map(a -> new BankAccountResponse(
                        a.getId(),
                        a.getName(),
                        a.getIban()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getContinuationKey(String userId, Long accountId) {
        return bankAccountRepository.findContinuationKey(accountId, userId);
    }

    @Transactional
    public void resetAccountSync(String userId, Long accountId) {
        int updated = bankAccountRepository.clearContinuationKey(accountId, userId);
        if (updated == 0) {
            throw new IllegalArgumentException("Account not found for this user");
        }

        bankTransactionRepository.deleteByAccountIdAndUserId(accountId, userId);
    }
}
