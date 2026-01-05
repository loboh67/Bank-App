package com.lobosoft.api.service;

import com.lobosoft.api.dto.BankSessionStatusResponse;
import com.lobosoft.api.model.BankSession;
import com.lobosoft.api.repository.BankSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankSessionService {

    private final BankSessionRepository bankSessionRepository;

    @Transactional(readOnly = true)
    public BankSessionStatusResponse getLatestSessionStatus(String userId) {
        OffsetDateTime now = OffsetDateTime.now();

        List<BankSession> validSessions = bankSessionRepository
                .findByUserIdAndValidUntilGreaterThanEqual(userId, now);

        Set<String> activeStatuses = Set.of("ACTIVE", "AUTHORIZED");

        var sessionIds = validSessions.stream()
                .filter(session -> activeStatuses.contains(
                        session.getStatus() != null ? session.getStatus().toUpperCase() : ""
                ))
                .map(BankSession::getId)
                .toList();

        boolean hasActive = !sessionIds.isEmpty();

        return new BankSessionStatusResponse(hasActive, sessionIds);
    }
}
