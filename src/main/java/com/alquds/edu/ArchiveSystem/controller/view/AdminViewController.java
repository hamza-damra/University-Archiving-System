package com.alquds.edu.ArchiveSystem.controller.view;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * View Controller for Admin Dashboard pages.
 * Maps clean URLs to static HTML files in /admin/ directory.
 * Authentication is handled client-side via JWT.
 * 
 * @author Archive System Team
 * @version 1.0
 * @since 2025-11-27
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminViewController {
    
    /**
     * Admin root - redirect to dashboard
     */
    @GetMapping({"", "/"})
    public String adminRoot() {
        log.info("Admin user accessing root - redirecting to dashboard");
        return "redirect:/admin/dashboard.html";
    }
    
    /**
     * Dashboard page
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        log.info("Admin user accessing dashboard page");
        return "redirect:/admin/dashboard.html";
    }
    
    /**
     * Users management page
     */
    @GetMapping("/users")
    public String users() {
        log.info("Admin user accessing users page");
        return "redirect:/admin/dashboard.html";
    }
    
    /**
     * Departments management page
     */
    @GetMapping("/departments")
    public String departments() {
        log.info("Admin user accessing departments page");
        return "redirect:/admin/dashboard.html";
    }
    
    /**
     * Courses management page
     */
    @GetMapping("/courses")
    public String courses() {
        log.info("Admin user accessing courses page");
        return "redirect:/admin/dashboard.html";
    }
    
    /**
     * Reports page
     */
    @GetMapping("/reports")
    public String reports() {
        log.info("Admin user accessing reports page");
        return "redirect:/admin/dashboard.html";
    }
}
