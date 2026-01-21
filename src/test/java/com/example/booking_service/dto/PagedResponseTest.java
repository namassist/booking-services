package com.example.booking_service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PagedResponseTest {

    @Test
    @DisplayName("from - Creates correct meta information")
    void from_CreatesCorrectMeta() {
        // Arrange
        List<String> content = List.of("item1", "item2", "item3");
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        Page<String> page = new PageImpl<>(content, pageable, 25);

        // Act
        PagedResponse<String> response = PagedResponse.from(page, "/api/test");

        // Assert
        assertThat(response.getData()).hasSize(3);
        assertThat(response.getMeta().getPage()).isEqualTo(0);
        assertThat(response.getMeta().getSize()).isEqualTo(10);
        assertThat(response.getMeta().getCount()).isEqualTo(3);
        assertThat(response.getMeta().getTotalItems()).isEqualTo(25);
        assertThat(response.getMeta().getTotalPages()).isEqualTo(3);
        assertThat(response.getMeta().isHasNext()).isTrue();
        assertThat(response.getMeta().isHasPrev()).isFalse();
    }

    @Test
    @DisplayName("from - Generates correct links")
    void from_GeneratesCorrectLinks() {
        // Arrange
        List<String> content = List.of("item1");
        PageRequest pageable = PageRequest.of(1, 10, Sort.by("name").ascending());
        Page<String> page = new PageImpl<>(content, pageable, 25);

        // Act
        PagedResponse<String> response = PagedResponse.from(page, "/api/test");

        // Assert
        assertThat(response.getLinks().getSelf()).contains("/api/test");
        assertThat(response.getLinks().getSelf()).contains("page=1");
        assertThat(response.getLinks().getNext()).contains("page=2");
        assertThat(response.getLinks().getPrev()).contains("page=0");
    }

    @Test
    @DisplayName("from - Handles empty page correctly")
    void from_HandlesEmptyPage() {
        // Arrange
        List<String> content = Collections.emptyList();
        PageRequest pageable = PageRequest.of(0, 10);
        Page<String> page = new PageImpl<>(content, pageable, 0);

        // Act
        PagedResponse<String> response = PagedResponse.from(page, "/api/test");

        // Assert
        assertThat(response.getData()).isEmpty();
        assertThat(response.getMeta().getCount()).isEqualTo(0);
        assertThat(response.getMeta().getTotalItems()).isEqualTo(0);
        assertThat(response.getMeta().getTotalPages()).isEqualTo(0);
        assertThat(response.getMeta().isHasNext()).isFalse();
        assertThat(response.getMeta().isHasPrev()).isFalse();
        assertThat(response.getLinks().getNext()).isNull();
        assertThat(response.getLinks().getPrev()).isNull();
    }
}
