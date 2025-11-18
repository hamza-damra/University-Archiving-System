package com.alqude.edu.ArchiveSystem.integration;

import com.alqude.edu.ArchiveSystem.config.DataInitializer;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for mock data with API endpoints.
 * Tests HOD and Professor access to data via API, filtering, search, and report generation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "mock.data.enabled=true",
    "mock.data.skip-if-exists=false"
})
@Transactional
@SuppressWarnings("null")
class MockDataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize mock data before each test
        dataInitializer.run();
    }

    /**
     * Test HOD can access all professors via API.
     * Verifies that HOD role can retrieve the list of professors.
     */
    @Test
    @WithMockUser(username = "hod.cs@alquds.edu", roles = "HOD")
    void testHODCanAccessAllProfessors() throws Exception {
        // When: HOD requests professor list
        mockMvc.perform(get("/api/hod/professors")
                .param("page", "0")
                .param("size", "50")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            // Then: Should return success with professor data
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
            .andExpect(jsonPath("$.data.totalElements").value(25));
    }

    /**
     * Test Professor can access their assignments via API.
     * Verifies that Professor role can retrieve their course assignments.
     */
    @Test
    void testProfessorCanAccessAssignedCourses() throws Exception {
        // Given: Get a professor user from mock data
        User professor = userRepository.findByRole(Role.ROLE_PROFESSOR).stream()
            .filter(User::getIsActive)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No active professor found"));

        // When: Professor requests their courses (using mock user with professor role)
        mockMvc.perform(get("/api/professor/dashboard/courses")
                .param("semesterId", "1")
                .with(request -> {
                    request.setRemoteUser(professor.getEmail());
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON))
            // Then: Should return success with course data
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * Test filtering professors by department.
     * Verifies that HOD can filter professors by department.
     */
    @Test
    @WithMockUser(username = "hod.cs@alquds.edu", roles = "HOD")
    void testFilteringProfessorsByDepartment() throws Exception {
        // When: HOD requests professors filtered by department
        mockMvc.perform(get("/api/hod/professors")
                .param("page", "0")
                .param("size", "50")
                .param("departmentId", "1")
                .contentType(MediaType.APPLICATION_JSON))
            // Then: Should return filtered results
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content.length()").value(greaterThan(0)));
    }

    /**
     * Test searching professors.
     * Verifies that HOD can search professors by name or email.
     */
    @Test
    @WithMockUser(username = "hod.cs@alquds.edu", roles = "HOD")
    void testSearchingProfessors() throws Exception {
        // When: HOD searches for professors
        mockMvc.perform(get("/api/hod/professors")
                .param("page", "0")
                .param("size", "50")
                .param("search", "prof")
                .contentType(MediaType.APPLICATION_JSON))
            // Then: Should return search results
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    /**
     * Test report generation with mock data.
     * Verifies that HOD can generate department submission reports.
     */
    @Test
    @WithMockUser(username = "hod.cs@alquds.edu", roles = "HOD")
    void testReportGenerationWithMockData() throws Exception {
        // When: HOD requests department report
        mockMvc.perform(get("/api/hod/reports/department")
                .param("departmentId", "1")
                .contentType(MediaType.APPLICATION_JSON))
            // Then: Should return report data
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists());
    }

    /**
     * Test professor dashboard overview.
     * Verifies that professor can access their dashboard overview with statistics.
     */
    @Test
    void testProfessorDashboardOverview() throws Exception {
        // Given: Get a professor user from mock data
        User professor = userRepository.findByRole(Role.ROLE_PROFESSOR).stream()
            .filter(User::getIsActive)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No active professor found"));

        // When: Professor requests dashboard overview
        mockMvc.perform(get("/api/professor/dashboard/overview")
                .param("semesterId", "1")
                .with(request -> {
                    request.setRemoteUser(professor.getEmail());
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON))
            // Then: Should return overview data
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists());
    }

    /**
     * Test HOD can access academic years.
     * Verifies that HOD can retrieve academic year information.
     */
    @Test
    @WithMockUser(username = "hod.cs@alquds.edu", roles = "HOD")
    void testHODCanAccessAcademicYears() throws Exception {
        // When: HOD requests academic years
        mockMvc.perform(get("/api/hod/academic-years")
                .contentType(MediaType.APPLICATION_JSON))
            // Then: Should return academic years
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(3));
    }

    /**
     * Test filtering submissions by status.
     * Verifies that filtering by submission status works with mock data.
     */
    @Test
    @WithMockUser(username = "hod.cs@alquds.edu", roles = "HOD")
    void testFilteringSubmissionsByStatus() throws Exception {
        // When: HOD requests submissions filtered by status
        mockMvc.perform(get("/api/hod/reports/submissions")
                .param("status", "UPLOADED")
                .contentType(MediaType.APPLICATION_JSON))
            // Then: Should return filtered submissions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
