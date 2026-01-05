package com.lobosoft.sync.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bank_sessions")
@Getter
@Setter
public class BankSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long userId;
    private String providerSessionId;
    private String status;

    private Instant createdAt;
    private Instant validUntil;
}
