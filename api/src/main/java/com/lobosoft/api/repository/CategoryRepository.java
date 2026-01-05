package com.lobosoft.api.repository;

import com.lobosoft.api.model.Category;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<@NonNull Category, @NonNull Long> {
    java.util.List<Category> findAllByOrderByNameAsc();
}
