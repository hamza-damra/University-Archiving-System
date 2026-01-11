package com.alquds.edu.ArchiveSystem.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for handling root path requests.
 * Always serves the login page (index.html) for the root path.
 * Authentication and role-based redirects are handled client-side via JWT.
 * 
 * This ensures that:
 * 1. The root path "/" always returns the login page
 * 2. No server-side redirects that could be cached by browsers
 * 3. Client-side JavaScript handles auth state and dashboard redirects
 * 
 * @author Archive System Team
 * @version 1.0
 * @since 2025-12-16
 */
@Controller
public class HomeController {

    /**
     * Handle root path - forward to index.html (login page)
     * Uses forward instead of redirect to avoid caching issues
     * 
     * @return Forward to index.html
     */
    @GetMapping("/")
    public String home() {
        // Forward (not redirect) to index.html to avoid browser caching redirects
        return "forward:/index.html";
    }
}

