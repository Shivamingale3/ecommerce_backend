package com.shivamingale.ecom.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivamingale.ecom.dto.request.CreateCategoryDTO;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.service.CategoryService;

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
}
