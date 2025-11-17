package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.Semester;
import com.alqude.edu.ArchiveSystem.entity.SemesterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    
    List<Semester> findByAcademicYearId(Long academicYearId);
    
    Optional<Semester> findByAcademicYearIdAndType(Long academicYearId, SemesterType type);
}
