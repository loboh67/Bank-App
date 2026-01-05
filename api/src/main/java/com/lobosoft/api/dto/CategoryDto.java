package com.lobosoft.api.dto;

public record CategoryDto(
        Long id,
        String key,
        String name,
        Long parentId
) {
}
