package com.alquds.edu.ArchiveSystem.service.academic;

import com.alquds.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.SemesterDTO;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.exception.core.DuplicateEntityException;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.AcademicYearRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AcademicService following Spring Boot Testing Best Practices.
 * 
 * Test Strategy:
 * - Mock all external dependencies (repositories)
 * - Test business logic in isolation
 * - Follow AAA pattern (Arrange, Act, Assert)
 * - Test edge cases and error conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AcademicService Unit Tests")
class AcademicServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;
    
    @Mock
    private SemesterRepository semesterRepository;
    
    @InjectMocks
    private AcademicServiceImpl academicService;
    
    private AcademicYear testAcademicYear;
    private AcademicYearDTO academicYearDTO;
    private Semester testSemester;
    private SemesterDTO semesterDTO;
    
    @BeforeEach
    void setUp() {
        // Arrange: Set up test data
        testAcademicYear = TestDataBuilder.createAcademicYear();
        testAcademicYear.setId(1L);
        
        academicYearDTO = TestDataBuilder.createAcademicYearDTO();
        
        testSemester = TestDataBuilder.createSemester();
        testSemester.setId(1L);
        testSemester.setAcademicYear(testAcademicYear);
        
        semesterDTO = TestDataBuilder.createSemesterDTO();
    }
    
    @Test
    @DisplayName("Should create academic year successfully")
    void shouldCreateAcademicYearSuccessfully() {
        // Arrange
        when(academicYearRepository.findByYearCode(anyString())).thenReturn(Optional.empty());
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> {
            AcademicYear year = invocation.getArgument(0);
            year.setId(1L);
            return year;
        });
        
        // Mock semester saves
        when(semesterRepository.save(any(Semester.class))).thenAnswer(invocation -> {
            Semester semester = invocation.getArgument(0);
            semester.setId(1L);
            return semester;
        });
        
        // Act
        AcademicYear result = academicService.createAcademicYear(academicYearDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getYearCode()).isEqualTo(academicYearDTO.getYearCode());
        assertThat(result.getStartYear()).isEqualTo(academicYearDTO.getStartYear());
        assertThat(result.getEndYear()).isEqualTo(academicYearDTO.getEndYear());
        assertThat(result.getIsActive()).isEqualTo(academicYearDTO.getIsActive());
        
        // Verify that three semesters were created
        verify(semesterRepository, times(3)).save(any(Semester.class));
        verify(academicYearRepository).save(any(AcademicYear.class));
    }
    
    @Test
    @DisplayName("Should throw DuplicateEntityException when creating academic year with duplicate year code")
    void shouldThrowDuplicateEntityExceptionWhenCreatingAcademicYearWithDuplicateYearCode() {
        // Arrange
        when(academicYearRepository.findByYearCode(academicYearDTO.getYearCode()))
                .thenReturn(Optional.of(testAcademicYear));
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.createAcademicYear(academicYearDTO))
                .isInstanceOf(DuplicateEntityException.class)
                .hasMessageContaining("already exists");
        
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
        verify(semesterRepository, never()).save(any(Semester.class));
    }
    
    @Test
    @DisplayName("Should throw IllegalArgumentException when end year is not greater than start year")
    void shouldThrowIllegalArgumentExceptionWhenEndYearNotGreaterThanStartYear() {
        // Arrange
        academicYearDTO.setEndYear(2024); // Same as start year
        when(academicYearRepository.findByYearCode(anyString())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.createAcademicYear(academicYearDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End year must be greater than start year");
        
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }
    
    @Test
    @DisplayName("Should update academic year successfully")
    void shouldUpdateAcademicYearSuccessfully() {
        // Arrange
        Long academicYearId = 1L;
        AcademicYearDTO updateDTO = new AcademicYearDTO();
        updateDTO.setYearCode("2025-2026");
        updateDTO.setStartYear(2025);
        updateDTO.setEndYear(2026);
        updateDTO.setIsActive(false);
        
        when(academicYearRepository.findById(academicYearId))
                .thenReturn(Optional.of(testAcademicYear));
        when(academicYearRepository.findByYearCode(updateDTO.getYearCode()))
                .thenReturn(Optional.empty());
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> {
            AcademicYear year = invocation.getArgument(0);
            year.setId(academicYearId);
            return year;
        });
        
        // Act
        AcademicYear result = academicService.updateAcademicYear(academicYearId, updateDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getYearCode()).isEqualTo(updateDTO.getYearCode());
        assertThat(result.getStartYear()).isEqualTo(updateDTO.getStartYear());
        assertThat(result.getEndYear()).isEqualTo(updateDTO.getEndYear());
        assertThat(result.getIsActive()).isEqualTo(updateDTO.getIsActive());
        
        verify(academicYearRepository).findById(academicYearId);
        verify(academicYearRepository).save(any(AcademicYear.class));
    }
    
    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent academic year")
    void shouldThrowEntityNotFoundExceptionWhenUpdatingNonExistentAcademicYear() {
        // Arrange
        Long academicYearId = 999L;
        when(academicYearRepository.findById(academicYearId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.updateAcademicYear(academicYearId, academicYearDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Academic year not found");
        
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }
    
    @Test
    @DisplayName("Should throw DuplicateEntityException when updating to duplicate year code")
    void shouldThrowDuplicateEntityExceptionWhenUpdatingToDuplicateYearCode() {
        // Arrange
        Long academicYearId = 1L;
        AcademicYearDTO updateDTO = new AcademicYearDTO();
        updateDTO.setYearCode("2025-2026"); // Different from existing
        updateDTO.setStartYear(2025);
        updateDTO.setEndYear(2026);
        
        AcademicYear existingYear = TestDataBuilder.createAcademicYear();
        existingYear.setId(2L);
        existingYear.setYearCode("2025-2026");
        
        when(academicYearRepository.findById(academicYearId))
                .thenReturn(Optional.of(testAcademicYear));
        when(academicYearRepository.findByYearCode(updateDTO.getYearCode()))
                .thenReturn(Optional.of(existingYear));
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.updateAcademicYear(academicYearId, updateDTO))
                .isInstanceOf(DuplicateEntityException.class)
                .hasMessageContaining("already exists");
        
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }
    
    @Test
    @DisplayName("Should return all academic years")
    void shouldGetAllAcademicYears() {
        // Arrange
        AcademicYear year1 = TestDataBuilder.createAcademicYear();
        year1.setId(1L);
        AcademicYear year2 = TestDataBuilder.createAcademicYear();
        year2.setId(2L);
        year2.setYearCode("2025-2026");
        year2.setStartYear(2025);
        year2.setEndYear(2026);
        
        List<AcademicYear> academicYears = new ArrayList<>();
        academicYears.add(year1);
        academicYears.add(year2);
        
        when(academicYearRepository.findAll()).thenReturn(academicYears);
        
        // Act
        List<AcademicYear> result = academicService.getAllAcademicYears();
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(year1, year2);
        
        verify(academicYearRepository).findAll();
    }
    
    @Test
    @DisplayName("Should return active academic year")
    void shouldGetActiveAcademicYear() {
        // Arrange
        when(academicYearRepository.findByIsActiveTrue())
                .thenReturn(Optional.of(testAcademicYear));
        
        // Act
        AcademicYear result = academicService.getActiveAcademicYear();
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getId()).isEqualTo(testAcademicYear.getId());
        
        verify(academicYearRepository).findByIsActiveTrue();
    }
    
    @Test
    @DisplayName("Should throw EntityNotFoundException when no active academic year exists")
    void shouldThrowEntityNotFoundExceptionWhenNoActiveAcademicYearExists() {
        // Arrange
        when(academicYearRepository.findByIsActiveTrue())
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.getActiveAcademicYear())
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No active academic year found");
        
        verify(academicYearRepository).findByIsActiveTrue();
    }
    
    @Test
    @DisplayName("Should set academic year as active and deactivate others")
    void shouldSetActiveAcademicYearAndDeactivateOthers() {
        // Arrange
        Long activeYearId = 1L;
        AcademicYear activeYear = TestDataBuilder.createAcademicYear();
        activeYear.setId(activeYearId);
        activeYear.setIsActive(false);
        
        AcademicYear otherYear = TestDataBuilder.createAcademicYear();
        otherYear.setId(2L);
        otherYear.setYearCode("2023-2024");
        otherYear.setStartYear(2023);
        otherYear.setEndYear(2024);
        otherYear.setIsActive(true);
        
        List<AcademicYear> allYears = new ArrayList<>();
        allYears.add(activeYear);
        allYears.add(otherYear);
        
        when(academicYearRepository.findById(activeYearId))
                .thenReturn(Optional.of(activeYear));
        when(academicYearRepository.findAll()).thenReturn(allYears);
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        academicService.setActiveAcademicYear(activeYearId);
        
        // Assert
        ArgumentCaptor<AcademicYear> captor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository, atLeastOnce()).save(captor.capture());
        
        List<AcademicYear> savedYears = captor.getAllValues();
        AcademicYear savedActiveYear = savedYears.stream()
                .filter(y -> y.getId().equals(activeYearId))
                .findFirst()
                .orElse(null);
        AcademicYear savedOtherYear = savedYears.stream()
                .filter(y -> y.getId().equals(otherYear.getId()))
                .findFirst()
                .orElse(null);
        
        assertThat(savedActiveYear).isNotNull();
        assertThat(savedActiveYear.getIsActive()).isTrue();
        
        if (savedOtherYear != null) {
            assertThat(savedOtherYear.getIsActive()).isFalse();
        }
        
        verify(academicYearRepository).findById(activeYearId);
        verify(academicYearRepository).findAll();
    }
    
    @Test
    @DisplayName("Should throw EntityNotFoundException when setting non-existent academic year as active")
    void shouldThrowEntityNotFoundExceptionWhenSettingNonExistentAcademicYearAsActive() {
        // Arrange
        Long academicYearId = 999L;
        when(academicYearRepository.findById(academicYearId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.setActiveAcademicYear(academicYearId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Academic year not found");
        
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }
    
    @Test
    @DisplayName("Should get semester by ID")
    void shouldGetSemesterById() {
        // Arrange
        Long semesterId = 1L;
        when(semesterRepository.findById(semesterId))
                .thenReturn(Optional.of(testSemester));
        
        // Act
        Semester result = academicService.getSemester(semesterId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(semesterId);
        assertThat(result.getType()).isEqualTo(testSemester.getType());
        
        verify(semesterRepository).findById(semesterId);
    }
    
    @Test
    @DisplayName("Should throw EntityNotFoundException when semester not found")
    void shouldThrowEntityNotFoundExceptionWhenSemesterNotFound() {
        // Arrange
        Long semesterId = 999L;
        when(semesterRepository.findById(semesterId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.getSemester(semesterId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Semester not found");
        
        verify(semesterRepository).findById(semesterId);
    }
    
    @Test
    @DisplayName("Should get semesters by academic year ID")
    void shouldGetSemestersByAcademicYearId() {
        // Arrange
        Long academicYearId = 1L;
        Semester semester1 = TestDataBuilder.createSemester();
        semester1.setId(1L);
        semester1.setType(SemesterType.FIRST);
        
        Semester semester2 = TestDataBuilder.createSemester();
        semester2.setId(2L);
        semester2.setType(SemesterType.SECOND);
        
        List<Semester> semesters = new ArrayList<>();
        semesters.add(semester1);
        semesters.add(semester2);
        
        when(academicYearRepository.existsById(academicYearId)).thenReturn(true);
        when(semesterRepository.findByAcademicYearId(academicYearId)).thenReturn(semesters);
        
        // Act
        List<Semester> result = academicService.getSemestersByYear(academicYearId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(semester1, semester2);
        
        verify(academicYearRepository).existsById(academicYearId);
        verify(semesterRepository).findByAcademicYearId(academicYearId);
    }
    
    @Test
    @DisplayName("Should throw EntityNotFoundException when academic year does not exist for getSemestersByYear")
    void shouldThrowEntityNotFoundExceptionWhenAcademicYearDoesNotExistForGetSemestersByYear() {
        // Arrange
        Long academicYearId = 999L;
        when(academicYearRepository.existsById(academicYearId)).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.getSemestersByYear(academicYearId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Academic year not found");
        
        verify(semesterRepository, never()).findByAcademicYearId(anyLong());
    }
    
    @Test
    @DisplayName("Should update semester successfully")
    void shouldUpdateSemesterSuccessfully() {
        // Arrange
        Long semesterId = 1L;
        SemesterDTO updateDTO = new SemesterDTO();
        updateDTO.setType(SemesterType.SECOND);
        updateDTO.setStartDate(LocalDate.of(2025, Month.FEBRUARY, 1));
        updateDTO.setEndDate(LocalDate.of(2025, Month.MAY, 31));
        updateDTO.setIsActive(false);
        
        when(semesterRepository.findById(semesterId))
                .thenReturn(Optional.of(testSemester));
        when(semesterRepository.save(any(Semester.class))).thenAnswer(invocation -> {
            Semester semester = invocation.getArgument(0);
            semester.setId(semesterId);
            return semester;
        });
        
        // Act
        Semester result = academicService.updateSemester(semesterId, updateDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(updateDTO.getType());
        assertThat(result.getStartDate()).isEqualTo(updateDTO.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(updateDTO.getEndDate());
        assertThat(result.getIsActive()).isEqualTo(updateDTO.getIsActive());
        
        verify(semesterRepository).findById(semesterId);
        verify(semesterRepository).save(any(Semester.class));
    }
    
    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent semester")
    void shouldThrowEntityNotFoundExceptionWhenUpdatingNonExistentSemester() {
        // Arrange
        Long semesterId = 999L;
        when(semesterRepository.findById(semesterId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.updateSemester(semesterId, semesterDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Semester not found");
        
        verify(semesterRepository, never()).save(any(Semester.class));
    }
    
    @Test
    @DisplayName("Should throw IllegalArgumentException when end date is before start date")
    void shouldThrowIllegalArgumentExceptionWhenEndDateIsBeforeStartDate() {
        // Arrange
        Long semesterId = 1L;
        SemesterDTO updateDTO = new SemesterDTO();
        updateDTO.setType(SemesterType.FIRST);
        updateDTO.setStartDate(LocalDate.of(2025, Month.MAY, 1));
        updateDTO.setEndDate(LocalDate.of(2025, Month.FEBRUARY, 1)); // End before start
        
        when(semesterRepository.findById(semesterId))
                .thenReturn(Optional.of(testSemester));
        
        // Act & Assert
        assertThatThrownBy(() -> academicService.updateSemester(semesterId, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");
        
        verify(semesterRepository, never()).save(any(Semester.class));
    }
}
