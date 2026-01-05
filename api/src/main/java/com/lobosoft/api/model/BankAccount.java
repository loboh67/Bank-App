package com.lobosoft.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "name")
    private String name;

    @Column(name = "iban")
    private String iban;

    @Column(name = "eb_continuation_key", columnDefinition = "text")
    private String ebContinuationKey;
}
