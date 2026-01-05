package com.lobosoft.api.service;

import com.lobosoft.api.dto.CategoryDto;
import com.lobosoft.api.model.Category;
import com.lobosoft.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getCategoryKey(),
                category.getName(),
                category.getParentId()
        );
    }
}
