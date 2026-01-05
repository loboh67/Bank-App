package com.lobosoft.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "bank_sessions")
@Getter
@Setter
public class BankSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "state")
    private String state;

    @Column(name = "aspsp_name")
    private String aspspName;

    @Column(name = "aspsp_country")
    private String aspspCountry;

    @Column(name = "status")
    private String status;
}
