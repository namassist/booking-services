package com.example.booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Metadata for paginated responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMeta {
    
    private int page;
    private int size;
    private int count;
    private long totalItems;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrev;
    private java.util.List<SortInfo> sort;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortInfo {
        private String field;
        private String direction;
    }
}
