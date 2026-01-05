package com.lobosoft.sync.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transactions",
uniqueConstraints = @UniqueConstraint(columnNames = {"bank_account_id", "provider_transaction_id"}))
@Getter
@Setter
public class BankTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    private BigDecimal amount;
    private String currency;
    private String direction;
    private LocalDate bookingDate;
    private LocalDate valueDate;

    @Column(name = "description_raw")
    private String descriptionRaw;

    @Column(name = "description_display")
    private String descriptionDisplay;

    @Column(name = "raw_json", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String rawJson;

    private Instant createdAt;
    private Instant updatedAt;
}
