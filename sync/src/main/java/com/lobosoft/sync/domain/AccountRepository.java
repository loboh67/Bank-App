package com.lobosoft.sync.domain;

import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface AccountRepository extends JpaRepository<@NonNull Account, @NonNull Long> {

    @Query("""
           select a
           from Account a
           where a.status = 'ACTIVE'
           """)
    List<Account> findActiveAndValid(Instant now);
}
