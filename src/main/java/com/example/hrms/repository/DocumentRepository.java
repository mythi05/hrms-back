package com.example.hrms.repository;

import com.example.hrms.entity.Document;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
        SELECT d FROM Document d
        WHERE (:category = 'all' OR d.category = :category)
        AND LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY d.createdAt DESC
    """)
    List<Document> filterDocuments(
            @Param("category") String category,
            @Param("search") String search
    );
}
