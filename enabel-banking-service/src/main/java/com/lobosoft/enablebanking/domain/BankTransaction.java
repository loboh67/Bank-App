package com.lobosoft.enablebanking.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@ToString
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "direction")
    private String direction;

    @Column(name = "currency")
    private String currency;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "description")
    private String description;
}
