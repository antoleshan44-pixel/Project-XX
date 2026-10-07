package com.urbano.monolith.blog.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.blog.dto.BlogCategoryDto;
import com.urbano.monolith.blog.dto.BlogPostDetailDto;
import com.urbano.monolith.blog.dto.BlogPostSummaryDto;
import com.urbano.monolith.blog.entity.BlogPost;
import com.urbano.monolith.blog.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<BlogPostSummaryDto> listPosts(String category, String search, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<BlogPost> result = repository.searchPublic(
                emptyToNull(category),
                emptyToNull(search),
                PageRequest.of(Math.max(page, 0), safeSize));

        List<BlogPostSummaryDto> content = result.getContent().stream()
                .map(this::toSummary).toList();

        return PagedResponse.<BlogPostSummaryDto>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public BlogPostDetailDto getBySlug(String slug) {
        BlogPost post = repository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + slug));
        return toDetail(post);
    }

    @Transactional(readOnly = true)
    public List<BlogCategoryDto> listCategories() {
        return repository.findDistinctCategories().stream()
                .map(name -> BlogCategoryDto.builder()
                        .name(name)
                        .slug(slugify(name))
                        .build())
                .toList();
    }

    private BlogPostSummaryDto toSummary(BlogPost p) {
        return BlogPostSummaryDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .category(p.getCategory())
                .summary(p.getSummary())
                .authorName(p.getAuthorName())
                .coverImageUrl(p.getCoverImageUrl())
                .publishedAt(p.getPublishedAt())
                .build();
    }

    private BlogPostDetailDto toDetail(BlogPost p) {
        return BlogPostDetailDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .category(p.getCategory())
                .summary(p.getSummary())
                .content(p.getContent())
                .authorName(p.getAuthorName())
                .coverImageUrl(p.getCoverImageUrl())
                .publishedAt(p.getPublishedAt())
                .build();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String slugify(String s) {
        return s == null ? null : s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}