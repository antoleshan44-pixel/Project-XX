package com.urbano.monolith.blog.controller;

import com.urbano.common.dto.PagedResponse;
import com.urbano.monolith.blog.dto.BlogCategoryDto;
import com.urbano.monolith.blog.dto.BlogPostDetailDto;
import com.urbano.monolith.blog.dto.BlogPostSummaryDto;
import com.urbano.monolith.blog.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/blog")
@RequiredArgsConstructor
public class PublicBlogController {

    private final BlogService blogService;

    @GetMapping("/posts")
    public ResponseEntity<PagedResponse<BlogPostSummaryDto>> listPosts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(blogService.listPosts(category, search, page, size));
    }

    @GetMapping("/posts/{slug}")
    public ResponseEntity<BlogPostDetailDto> getPost(@PathVariable("slug") String slug) {
        return ResponseEntity.ok(blogService.getBySlug(slug));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<BlogCategoryDto>> listCategories() {
        return ResponseEntity.ok(blogService.listCategories());
    }
}