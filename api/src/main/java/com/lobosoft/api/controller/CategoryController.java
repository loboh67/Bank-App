package com.lobosoft.api.controller;

import com.lobosoft.api.dto.CategoryDto;
import com.lobosoft.api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public List<CategoryDto> listCategories(Authentication auth) {
        String userId = getUserId(auth);
        log.info("User {} -> GET /categories", userId);
        return categoryService.listCategories();
    }

    private String getUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authentication");
        }
        String userId = auth.getPrincipal().toString();
        if ("anonymousUser".equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Anonymous user");
        }
        return userId;
    }
}
