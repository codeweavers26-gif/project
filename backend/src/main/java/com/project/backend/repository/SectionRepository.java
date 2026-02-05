package com.project.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Section;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByIsActiveTrue();

	boolean existsByNameIgnoreCase(String name);
}

