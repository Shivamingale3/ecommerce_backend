package com.shivamingale.ecom.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaginationRequestDTO {

    @Min(value = 0, message = "Page number must be 0 or greater")
    private int pageNumber = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private int pageSize = 10;

    @NotBlank(message = "Sort by is required")
    private String sortBy = "createdAt";

    @Size(max = 100, message = "Search term must be at most 100 characters")
    private String categoryName;

    /**
     * Sort order: 0 = descending (default), 1 = ascending
     */
    @Min(value = 0, message = "Sort order must be 0 (desc) or 1 (asc)")
    @Max(value = 1, message = "Sort order must be 0 (desc) or 1 (asc)")
    private int sortOrder = 0;

    public Pageable toPageable() {
        Sort sort = sortOrder == 1
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
