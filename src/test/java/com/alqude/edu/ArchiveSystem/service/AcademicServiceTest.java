package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alqude.edu.ArchiveSystem.entity.AcademicYear;
import com.alqude.edu.ArchiveSystem.entity.Semester;
import com.alqude.edu.ArchiveSystem.entity.SemesterType;
import com.alqude.edu.ArchiveSystem.exception.DuplicateEntityException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.AcademicYearRepository;
import com.alqude.edu.ArchiveSystem.repository.SemesterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AcademicServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @InjectMocks
    private AcademicServiceImpl academicService;

    private AcademicYearDTO academicYearDTO;
    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        academicYearDTO = new AcademicYearDTO();
        academicYearDTO.setYearCode("2024-2025");
        academicYearDTO.setStartYear(2024);
        academicYearDTO.setEndYear(2025);
        academicYearDTO.setIsActive(false);

        academicYear = new AcademicYear();
        academicYear.setId(1L);
        academicYear.setYearCode("2024-2025");
        academicYear.setStartYear(2024);
        academicYear.setEndYear(2025);
        academicYear.setIsActive(false);
    }

    @Test
    void testCreateAcademicYear_AutoCreatesThreeSemesters() {
        // Arrange
        when(academicYearRepository.findByYearCode(academicYearDTO.getYearCode())).thenReturn(Optional.empty());
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(academicYear);
        
        // Mock semester creation
        Semester firstSemester = createSemester(1L, academicYear, SemesterType.FIRST, 2024);
        Semester secondSemester = createSemester(2L, academicYear, SemesterType.SECOND, 2025);
        Semester summerSemester = createSemester(3L, academicYear, SemesterType.SUMMER, 2025);
        
        when(semesterRepository.save(any(Semester.class)))
            .thenReturn(firstSemester)
            .thenReturn(secondSemester)
            .thenReturn(summerSemester);

        // Act
        AcademicYear result = academicService.createAcademicYear(academicYearDTO);

        // Assert
        assertNotNull(result);
        assertEquals("2024-2025", result.getYearCode());
        assertEquals(2024, result.getStartYear());
        assertEquals(2025, result.getEndYear());
        
        // Verify that three semesters were created
        verify(semesterRepository, times(3)).save(any(Semester.class));
        
        // Verify semester types
        assertNotNull(result.getSemesters());
        assertEquals(3, result.getSemesters().size());
        
        List<SemesterType> semesterTypes = result.getSemesters().stream()
            .map(Semester::getType)
            .toList();
        assertTrue(semesterTypes.contains(SemesterType.FIRST));
        assertTrue(semesterTypes.contains(SemesterType.SECOND));
        assertTrue(semesterTypes.contains(SemesterType.SUMMER));
    }

    @Test
    void testCreateAcademicYear_ThrowsExceptionWhenYearCodeExists() {
        // Arrange
        when(academicYearRepository.findByYearCode(academicYearDTO.getYearCode()))
            .thenReturn(Optional.of(academicYear));

        // Act & Assert
        assertThrows(DuplicateEntityException.class, () -> {
            academicService.createAcademicYear(academicYearDTO);
        });
        
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
        verify(semesterRepository, never()).save(any(Semester.class));
    }

    @Test
    void testCreateAcademicYear_ThrowsExceptionWhenEndYearNotGreaterThanStartYear() {
        // Arrange
        academicYearDTO.setEndYear(2024); // Same as start year
        when(academicYearRepository.findByYearCode(academicYearDTO.getYearCode())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            academicService.createAcademicYear(academicYearDTO);
        });
        
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void testSetActiveAcademicYear_DeactivatesOtherYears() {
        // Arrange
        Long targetYearId = 2L;
        
        AcademicYear year1 = new AcademicYear();
        year1.setId(1L);
        year1.setYearCode("2023-2024");
        year1.setIsActive(true);
        
        AcademicYear year2 = new AcademicYear();
        year2.setId(2L);
        year2.setYearCode("2024-2025");
        year2.setIsActive(false);
        
        AcademicYear year3 = new AcademicYear();
        year3.setId(3L);
        year3.setYearCode("2025-2026");
        year3.setIsActive(false);
        
        List<AcademicYear> allYears = List.of(year1, year2, year3);
        
        when(academicYearRepository.findById(targetYearId)).thenReturn(Optional.of(year2));
        when(academicYearRepository.findAll()).thenReturn(allYears);
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        academicService.setActiveAcademicYear(targetYearId);

        // Assert
        // Verify year1 was deactivated
        assertFalse(year1.getIsActive());
        
        // Verify year2 was activated
        assertTrue(year2.getIsActive());
        
        // Verify year3 remains inactive
        assertFalse(year3.getIsActive());
        
        // Verify save was called for year1 (deactivation) and year2 (activation)
        verify(academicYearRepository, times(2)).save(any(AcademicYear.class));
    }

    @Test
    void testSetActiveAcademicYear_ThrowsExceptionWhenYearNotFound() {
        // Arrange
        Long nonExistentYearId = 999L;
        when(academicYearRepository.findById(nonExistentYearId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            academicService.setActiveAcademicYear(nonExistentYearId);
        });
        
        verify(academicYearRepository, never()).findAll();
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void testGetActiveAcademicYear_ReturnsActiveYear() {
        // Arrange
        academicYear.setIsActive(true);
        when(academicYearRepository.findByIsActiveTrue()).thenReturn(Optional.of(academicYear));

        // Act
        AcademicYear result = academicService.getActiveAcademicYear();

        // Assert
        assertNotNull(result);
        assertEquals("2024-2025", result.getYearCode());
        assertTrue(result.getIsActive());
    }

    @Test
    void testGetActiveAcademicYear_ThrowsExceptionWhenNoActiveYear() {
        // Arrange
        when(academicYearRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            academicService.getActiveAcademicYear();
        });
    }

    private Semester createSemester(Long id, AcademicYear academicYear, SemesterType type, int year) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.setAcademicYear(academicYear);
        semester.setType(type);
        semester.setIsActive(true);
        
        switch (type) {
            case FIRST:
                semester.setStartDate(LocalDate.of(year, Month.SEPTEMBER, 1));
                semester.setEndDate(LocalDate.of(year, Month.DECEMBER, 31));
                break;
            case SECOND:
                semester.setStartDate(LocalDate.of(year, Month.FEBRUARY, 1));
                semester.setEndDate(LocalDate.of(year, Month.MAY, 31));
                break;
            case SUMMER:
                semester.setStartDate(LocalDate.of(year, Month.JUNE, 1));
                semester.setEndDate(LocalDate.of(year, Month.AUGUST, 31));
                break;
        }
        
        return semester;
    }
}
