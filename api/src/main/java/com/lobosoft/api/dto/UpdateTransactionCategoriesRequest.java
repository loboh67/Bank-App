package com.lobosoft.api.dto;

import java.util.List;

public record UpdateTransactionCategoriesRequest(List<CategorySelection> categories) {

    public record CategorySelection(
            Long categoryId,
            Boolean primary
    ) {
    }
}
