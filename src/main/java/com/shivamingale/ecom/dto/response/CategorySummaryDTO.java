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
public class CategorySummaryDTO {

    private String id;
    private String name;
    private String slug;
    private MediaDTO image;
    private CategorySummaryDTO parent;
    private List<CategorySummaryDTO> children;

    /**
     * Maps a Category entity to a DTO with full one-level nesting.
     * - parent is mapped shallowly (no children, no further parent)
     * - children are mapped shallowly (no parent back-reference, no grandchildren)
     *
     * This prevents infinite recursion caused by bidirectional Category
     * relationships.
     */
    public static CategorySummaryDTO fromEntity(Category category) {
        if (category == null)
            return null;

        CategorySummaryDTO dto = CategorySummaryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .image(MediaDTO.fromEntity(category.getImage()))
                .build();

        // Map parent shallowly — only id, name, slug, image; no parent-of-parent or
        // children
        if (category.getParent() != null) {
            dto.setParent(fromEntityShallow(category.getParent()));
        }

        // Map children shallowly — only id, name, slug, image; no back-reference to
        // parent
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            dto.setChildren(
                    category.getChildren().stream()
                            .map(CategorySummaryDTO::fromEntityShallow)
                            .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Shallow mapping: only scalar fields and image — no parent or children.
     * Used to break recursive cycles.
     */
    public static CategorySummaryDTO fromEntityShallow(Category category) {
        if (category == null)
            return null;
        return CategorySummaryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .image(MediaDTO.fromEntity(category.getImage()))
                .build();
    }

    public static List<CategorySummaryDTO> fromEntities(List<Category> categories) {
        if (categories == null)
            return null;
        return categories.stream()
                .map(CategorySummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }
}