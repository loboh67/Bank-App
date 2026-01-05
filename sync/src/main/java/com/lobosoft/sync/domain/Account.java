package com.lobosoft.sync.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bank_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider_account_id"}))
@Getter
@Setter
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "provider_account_id")
    private String providerAccountId;

    private String identificationHash;
    private String iban;
    private String status;

    @Column(columnDefinition = "text")
    private String ebContinuationKey;

    private Instant createdAt;
}
