package com.lobosoft.api.dto;

import java.util.List;

public record BankSessionStatusResponse(
        boolean hasActiveSessions,
        List<Long> sessionIds
) {
}
