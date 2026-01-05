package com.lobosoft.api.repository;

import com.lobosoft.api.model.Merchant;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<@NonNull Merchant, @NonNull Long> {
}
