package com.example.booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic paginated response wrapper with meta and links.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    
    private List<T> data;
    private PageMeta meta;
    private PageLinks links;

    /**
     * Create a PagedResponse from a Spring Data Page.
     * 
     * @param page The page data
     * @param basePath The base API path (e.g., "/api/doctors")
     */
    public static <T> PagedResponse<T> from(Page<T> page, String basePath) {
        int currentPage = page.getNumber();
        int size = page.getSize();
        int totalPages = page.getTotalPages();

        // Build meta
        PageMeta meta = PageMeta.builder()
                .page(currentPage)
                .size(size)
                .count(page.getNumberOfElements())
                .totalItems(page.getTotalElements())
                .totalPages(totalPages)
                .hasNext(page.hasNext())
                .hasPrev(page.hasPrevious())
                .build();

        // Build sort query string
        String sortQuery = page.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .collect(Collectors.joining("&sort="));
        String sortParam = sortQuery.isEmpty() ? "" : "&sort=" + sortQuery;

        // Build links
        String selfUrl = String.format("%s?page=%d&size=%d%s", basePath, currentPage, size, sortParam);
        String nextUrl = page.hasNext() 
                ? String.format("%s?page=%d&size=%d%s", basePath, currentPage + 1, size, sortParam) 
                : null;
        String prevUrl = page.hasPrevious() 
                ? String.format("%s?page=%d&size=%d%s", basePath, currentPage - 1, size, sortParam) 
                : null;

        PageLinks links = PageLinks.builder()
                .self(selfUrl)
                .next(nextUrl)
                .prev(prevUrl)
                .build();

        return PagedResponse.<T>builder()
                .data(page.getContent())
                .meta(meta)
                .links(links)
                .build();
    }

    /**
     * Create a PagedResponse from a Spring Data Page without links (simpler version).
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return from(page, "");
    }
}
