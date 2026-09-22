package com.example.store.dto;

import lombok.Data;

import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    // source is 0-indexed (Spring Data), page here is 1-indexed
    public static <T> PageResponse<T> of(List<T> content, Page<?> source) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        response.setPage(source.getNumber() + 1);
        response.setSize(source.getSize());
        response.setTotalElements(source.getTotalElements());
        response.setTotalPages(source.getTotalPages());
        return response;
    }
}
