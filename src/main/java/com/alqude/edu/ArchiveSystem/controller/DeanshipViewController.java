package com.alqude.edu.ArchiveSystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * View Controller for Deanship Dashboard single-page application.
 * All routes redirect to the unified deanship-dashboard.html
 * Authentication is handled client-side via JWT.
 * 
 * @author Archive System Team
 * @version 2.0
 * @since 2025-11-20
 */
@Controller
@RequestMapping("/deanship")
@RequiredArgsConstructor
@Slf4j
public class DeanshipViewController {
    
    /**
     * Main dashboard landing page (single-page application)
     * Serves the unified deanship-dashboard.html
     * 
     * @return Redirect to deanship-dashboard.html
     */
    @GetMapping({"", "/"})
    public String deanshipRoot() {
        log.info("Deanship user accessing dashboard");
        return "redirect:/deanship-dashboard.html";
    }
    
    /**
     * Legacy route - redirect to main dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        log.info("Deanship user accessing dashboard page (legacy route)");
        return "redirect:/deanship-dashboard.html";
    }
    
    // Remove old multi-page routes - now using single-page application
    // All routes below redirect to the unified dashboard
    
    @GetMapping("/academic-years")
    public String academicYears() {
        return "redirect:/deanship-dashboard.html";
    }
    
    @GetMapping("/professors")
    public String professors() {
        return "redirect:/deanship-dashboard.html";
    }
    
    @GetMapping("/courses")
    public String courses() {
        return "redirect:/deanship-dashboard.html";
    }
    
    @GetMapping("/course-assignments")
    public String courseAssignments() {
        return "redirect:/deanship-dashboard.html";
    }
    
    @GetMapping("/reports")
    public String reports() {
        return "redirect:/deanship-dashboard.html";
    }
    
    @GetMapping("/file-explorer")
    public String fileExplorer() {
        return "redirect:/deanship-dashboard.html";
    }
}
