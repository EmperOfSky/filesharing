package com.filesharing.repository;

import com.filesharing.entity.CollaborativeDocument;
import com.filesharing.entity.CollaborativeDocumentSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollaborativeDocumentSnapshotRepository extends JpaRepository<CollaborativeDocumentSnapshot, Long> {

    Page<CollaborativeDocumentSnapshot> findByDocumentOrderByCreatedAtDesc(CollaborativeDocument document, Pageable pageable);

    Optional<CollaborativeDocumentSnapshot> findTopByDocumentOrderByVersionNumberDesc(CollaborativeDocument document);

    void deleteByDocument(CollaborativeDocument document);
}
