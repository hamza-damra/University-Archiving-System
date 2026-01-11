package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;

import com.alquds.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.SemesterDTO;

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
