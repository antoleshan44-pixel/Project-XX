package com.urbano.monolith.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostDetailDto {
    private UUID id;
    private String title;
    private String slug;
    private String category;
    private String summary;
    private String content;
    private String authorName;
    private String coverImageUrl;
    private LocalDateTime publishedAt;
}