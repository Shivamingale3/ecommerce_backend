package com.shivamingale.ecom.dto.response;

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
public class CategoryDTO {

    private String id;
    private String name;
    private String slug;
    private String description;
    private Integer displayOrder;
    private boolean active;
    private String path;
    private Integer depth;
    private CategorySummaryDTO parent;
    private List<CategorySummaryDTO> children;
    private MediaDTO image;

    public static CategoryDTO fromEntity(Category category) {
        if (category == null)
            return null;
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .active(category.isActive())
                .path(category.getPath())
                .depth(category.getDepth())
                .parent(CategorySummaryDTO.fromEntity(category.getParent()))
                .children(category.getChildren() != null
                        ? category.getChildren().stream()
                                .map(CategorySummaryDTO::fromEntity)
                                .collect(Collectors.toList())
                        : null)
                .image(MediaDTO.fromEntity(category.getImage()))
                .build();
    }
}