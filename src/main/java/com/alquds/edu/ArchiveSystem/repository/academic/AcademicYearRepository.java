package com.alquds.edu.ArchiveSystem.repository.academic;

import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    
    Optional<AcademicYear> findByYearCode(String yearCode);
    
    Optional<AcademicYear> findByIsActiveTrue();
}
