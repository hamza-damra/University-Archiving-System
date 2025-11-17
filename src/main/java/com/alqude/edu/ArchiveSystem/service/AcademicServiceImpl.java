package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.SemesterDTO;
import com.alqude.edu.ArchiveSystem.entity.AcademicYear;
import com.alqude.edu.ArchiveSystem.entity.Semester;
import com.alqude.edu.ArchiveSystem.entity.SemesterType;
import com.alqude.edu.ArchiveSystem.exception.DuplicateEntityException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.AcademicYearRepository;
import com.alqude.edu.ArchiveSystem.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicServiceImpl implements AcademicService {
    
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    
    @Override
    @Transactional
    public AcademicYear createAcademicYear(AcademicYearDTO dto) {
        log.info("Creating academic year: {}", dto.getYearCode());
        
        // Check if year code already exists
        if (academicYearRepository.findByYearCode(dto.getYearCode()).isPresent()) {
            throw new DuplicateEntityException("Academic year with code " + dto.getYearCode() + " already exists");
        }
        
        // Validate year range
        if (dto.getEndYear() <= dto.getStartYear()) {
            throw new IllegalArgumentException("End year must be greater than start year");
        }
        
        // Create academic year
        AcademicYear academicYear = new AcademicYear();
        academicYear.setYearCode(dto.getYearCode());
        academicYear.setStartYear(dto.getStartYear());
        academicYear.setEndYear(dto.getEndYear());
        academicYear.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false);
        
        // Save academic year first to get the ID
        academicYear = academicYearRepository.save(academicYear);
        
        // Automatically create three semesters
        List<Semester> semesters = createDefaultSemesters(academicYear);
        academicYear.setSemesters(semesters);
        
        log.info("Successfully created academic year {} with {} semesters", 
                academicYear.getYearCode(), semesters.size());
        
        return academicYear;
    }
    
    /**
     * Creates three default semesters (FIRST, SECOND, SUMMER) for an academic year
     * with default date ranges based on typical academic calendar
     */
    private List<Semester> createDefaultSemesters(AcademicYear academicYear) {
        List<Semester> semesters = new ArrayList<>();
        
        // First Semester (Fall): September to December
        Semester firstSemester = new Semester();
        firstSemester.setAcademicYear(academicYear);
        firstSemester.setType(SemesterType.FIRST);
        firstSemester.setStartDate(LocalDate.of(academicYear.getStartYear(), Month.SEPTEMBER, 1));
        firstSemester.setEndDate(LocalDate.of(academicYear.getStartYear(), Month.DECEMBER, 31));
        firstSemester.setIsActive(true);
        semesters.add(semesterRepository.save(firstSemester));
        
        // Second Semester (Spring): February to May
        Semester secondSemester = new Semester();
        secondSemester.setAcademicYear(academicYear);
        secondSemester.setType(SemesterType.SECOND);
        secondSemester.setStartDate(LocalDate.of(academicYear.getEndYear(), Month.FEBRUARY, 1));
        secondSemester.setEndDate(LocalDate.of(academicYear.getEndYear(), Month.MAY, 31));
        secondSemester.setIsActive(true);
        semesters.add(semesterRepository.save(secondSemester));
        
        // Summer Semester: June to August
        Semester summerSemester = new Semester();
        summerSemester.setAcademicYear(academicYear);
        summerSemester.setType(SemesterType.SUMMER);
        summerSemester.setStartDate(LocalDate.of(academicYear.getEndYear(), Month.JUNE, 1));
        summerSemester.setEndDate(LocalDate.of(academicYear.getEndYear(), Month.AUGUST, 31));
        summerSemester.setIsActive(true);
        semesters.add(semesterRepository.save(summerSemester));
        
        log.debug("Created {} semesters for academic year {}", semesters.size(), academicYear.getYearCode());
        
        return semesters;
    }
    
    @Override
    @Transactional
    public AcademicYear updateAcademicYear(Long id, AcademicYearDTO dto) {
        log.info("Updating academic year with ID: {}", id);
        
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found with ID: " + id));
        
        // Check if year code is being changed and if it conflicts with existing
        if (!academicYear.getYearCode().equals(dto.getYearCode())) {
            if (academicYearRepository.findByYearCode(dto.getYearCode()).isPresent()) {
                throw new DuplicateEntityException("Academic year with code " + dto.getYearCode() + " already exists");
            }
            academicYear.setYearCode(dto.getYearCode());
        }
        
        // Validate year range
        if (dto.getEndYear() <= dto.getStartYear()) {
            throw new IllegalArgumentException("End year must be greater than start year");
        }
        
        academicYear.setStartYear(dto.getStartYear());
        academicYear.setEndYear(dto.getEndYear());
        
        if (dto.getIsActive() != null) {
            academicYear.setIsActive(dto.getIsActive());
        }
        
        academicYear = academicYearRepository.save(academicYear);
        
        log.info("Successfully updated academic year: {}", academicYear.getYearCode());
        
        return academicYear;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AcademicYear> getAllAcademicYears() {
        log.debug("Fetching all academic years");
        return academicYearRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public AcademicYear getActiveAcademicYear() {
        log.debug("Fetching active academic year");
        return academicYearRepository.findByIsActiveTrue()
                .orElseThrow(() -> new EntityNotFoundException("No active academic year found"));
    }
    
    @Override
    @Transactional
    public void setActiveAcademicYear(Long id) {
        log.info("Setting academic year {} as active", id);
        
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found with ID: " + id));
        
        // Deactivate all other academic years
        List<AcademicYear> allYears = academicYearRepository.findAll();
        for (AcademicYear year : allYears) {
            if (!year.getId().equals(id) && year.getIsActive()) {
                year.setIsActive(false);
                academicYearRepository.save(year);
            }
        }
        
        // Activate the selected year
        academicYear.setIsActive(true);
        academicYearRepository.save(academicYear);
        
        log.info("Successfully set academic year {} as active", academicYear.getYearCode());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Semester getSemester(Long semesterId) {
        log.debug("Fetching semester with ID: {}", semesterId);
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with ID: " + semesterId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Semester> getSemestersByYear(Long academicYearId) {
        log.debug("Fetching semesters for academic year ID: {}", academicYearId);
        
        // Verify academic year exists
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new EntityNotFoundException("Academic year not found with ID: " + academicYearId);
        }
        
        return semesterRepository.findByAcademicYearId(academicYearId);
    }
    
    @Override
    @Transactional
    public Semester updateSemester(Long id, SemesterDTO dto) {
        log.info("Updating semester with ID: {}", id);
        
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with ID: " + id));
        
        // Validate date range
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        
        semester.setType(dto.getType());
        semester.setStartDate(dto.getStartDate());
        semester.setEndDate(dto.getEndDate());
        
        if (dto.getIsActive() != null) {
            semester.setIsActive(dto.getIsActive());
        }
        
        semester = semesterRepository.save(semester);
        
        log.info("Successfully updated semester: {} for academic year {}", 
                semester.getType(), semester.getAcademicYear().getYearCode());
        
        return semester;
    }
}
