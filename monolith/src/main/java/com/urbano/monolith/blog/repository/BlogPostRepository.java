package com.urbano.monolith.blog.repository;

import com.urbano.monolith.blog.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);

    Page<BlogPost> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    Page<BlogPost> findByCategoryAndPublishedTrueOrderByPublishedAtDesc(
            String category, Pageable pageable);

    @Query("""
        SELECT p FROM BlogPost p
        WHERE p.published = true
          AND (:category IS NULL OR p.category = :category)
          AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(p.summary) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY p.publishedAt DESC
    """)
    Page<BlogPost> searchPublic(
            @Param("category") String category,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM BlogPost p WHERE p.published = true ORDER BY p.category")
    List<String> findDistinctCategories();
}