package com.shivamingale.ecom.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.Category;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategorySummaryDTO {

    private String id;
    private String name;
    private String slug;
    private MediaDTO image;

    public static CategorySummaryDTO fromEntity(Category category) {
        if (category == null) return null;
        return CategorySummaryDTO.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .image(MediaDTO.fromEntity(category.getImage()))
            .build();
    }

    public static List<CategorySummaryDTO> fromEntities(List<Category> categories) {
        if (categories == null) return null;
        return categories.stream()
            .map(CategorySummaryDTO::fromEntity)
            .collect(Collectors.toList());
    }
}