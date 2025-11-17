package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.SemesterDTO;
import com.alqude.edu.ArchiveSystem.entity.AcademicYear;
import com.alqude.edu.ArchiveSystem.entity.Semester;

import java.util.List;

public interface AcademicService {
    
    // Academic Year Management
    AcademicYear createAcademicYear(AcademicYearDTO dto);
    
    AcademicYear updateAcademicYear(Long id, AcademicYearDTO dto);
    
    List<AcademicYear> getAllAcademicYears();
    
    AcademicYear getActiveAcademicYear();
    
    void setActiveAcademicYear(Long id);
    
    // Semester Management
    Semester getSemester(Long semesterId);
    
    List<Semester> getSemestersByYear(Long academicYearId);
    
    Semester updateSemester(Long id, SemesterDTO dto);
}
