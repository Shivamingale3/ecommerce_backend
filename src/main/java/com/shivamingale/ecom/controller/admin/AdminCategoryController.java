package com.shivamingale.ecom.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivamingale.ecom.dto.request.CreateCategoryDTO;
import com.shivamingale.ecom.dto.request.PaginationRequestDTO;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.dto.response.CategoryDTO;
import com.shivamingale.ecom.dto.response.CategorySummaryDTO;
import com.shivamingale.ecom.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public ResponseEntity<AppResponse<Void>> createCategory(@RequestBody CreateCategoryDTO request) {
        categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.success(null, "Category created successfully", HttpStatus.CREATED));
    }

    @GetMapping
    public ResponseEntity<AppResponse<Page<CategorySummaryDTO>>> getCategoriesByPagination(
            @Valid @ModelAttribute PaginationRequestDTO pagination) {
        Page<CategorySummaryDTO> categories = categoryService.getAllCategoriesByPagination(pagination);
        return ResponseEntity.ok(AppResponse.success(categories, "Categories fetched successfully", HttpStatus.OK));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppResponse<CategoryDTO>> getCategoryById(@PathVariable String id) {
        CategoryDTO category = categoryService.getByCategoryId(id, false);
        return ResponseEntity.ok(AppResponse.success(category, "Category fetched successfully", HttpStatus.OK));
    }

}
