package com.lobosoft.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key")
    private String categoryKey;

    @Column(name = "name")
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
