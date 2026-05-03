package com.shivamingale.ecom.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivamingale.ecom.dto.request.CreateCategoryDTO;
import com.shivamingale.ecom.dto.request.PaginationRequestDTO;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.dto.response.CategoryDTO;
import com.shivamingale.ecom.dto.response.CategorySummaryDTO;
import com.shivamingale.ecom.entity.Category;
import com.shivamingale.ecom.entity.Media;
import com.shivamingale.ecom.enums.MediaRole;
import com.shivamingale.ecom.exception.AppException;
import com.shivamingale.ecom.repository.CategoryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MediaService mediaService;

    public void createCategory(CreateCategoryDTO categoryData) {
        String normalizedName = categoryData.getName().trim().toLowerCase(Locale.ROOT);
        String normalizedSlug = categoryData.getSlug().trim().toLowerCase(Locale.ROOT);
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new AppException(HttpStatus.CONFLICT, "Category with this name already exists");
        }
        if (categoryRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new AppException(HttpStatus.CONFLICT, "Category with this slug already exists");
        }

        Category newCategory = new Category();
        newCategory.setName(categoryData.getName());
        newCategory.setSlug(categoryData.getSlug());

        if (categoryData.getDescription() != null) {
            newCategory.setDescription(categoryData.getDescription());
        }

        newCategory.setDepth(0);

        if (categoryData.getPath() != null && !categoryData.getPath().isBlank()) {
            newCategory.setPath(categoryData.getPath().trim());
        }

        if (categoryData.getParentCategoryId() != null) {
            Category parent = categoryRepository.findById(categoryData.getParentCategoryId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Parent category not found"));
            if (!parent.isActive()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Parent category is not active");
            }

            if (parent.getParent() != null) {
                newCategory.setDepth(parent.getDepth() + 1);
            }
            newCategory.setParent(parent);
        }

        Media tempMedia = null;
        if (categoryData.getTempMediaId() != null) {
            tempMedia = mediaService.findById(categoryData.getTempMediaId(), false);
            newCategory.setImage(tempMedia);
        }

        Category result = categoryRepository.save(newCategory);
        if (result != null) {
            log.info("Category created successfully {}", result.getName());
            if (tempMedia != null) {
                String key = mediaService.getUploadKeyByPurpose(MediaRole.CATEGORY, result.getId(),
                        tempMedia.getName());
                mediaService.disposeTempMedia(tempMedia, key);
            }
        } else {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create category");
        }
    }

    public Page<CategorySummaryDTO> getAllCategoriesByPagination(PaginationRequestDTO pagination) {
        Pageable pageable = pagination.toPageable();
        String search = pagination.getCategoryName();

        Page<Category> page = (search != null && !search.isBlank())
                ? categoryRepository.findByActiveTrueAndDeletedFalseAndNameContainingIgnoreCase(
                        search.trim(), pageable)
                : categoryRepository.findByActiveTrueAndDeletedFalse(pageable);

        return page.map(CategorySummaryDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public CategoryDTO getByCategoryId(String categoryId, boolean nullable) {
        if (nullable) {
            return CategoryDTO.fromEntity(categoryRepository.findById(categoryId).orElse(null));
        }
        return CategoryDTO.fromEntity(categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Category not found")));
    }

    public void deleteCategory(String categoryId) {
        if (categoryRepository.existsById(categoryId)) {
            categoryRepository.deleteById(categoryId);
        }
        throw new AppException(HttpStatus.NOT_FOUND, "Category not found");
    }

    public ResponseEntity<AppResponse<Void>> updateCategory() {
        return ResponseEntity.ok(AppResponse.success(null, "Category updated successfully", HttpStatus.OK));
    }
}
