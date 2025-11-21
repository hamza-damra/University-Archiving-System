package com.alqude.edu.ArchiveSystem.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes.
 * Run this as a standalone Java application to generate a BCrypt hash.
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";

        // Generate hash
        String hashedPassword = encoder.encode(password);

        System.out.println("==================================================");
        System.out.println("BCrypt Password Hash Generator");
        System.out.println("==================================================");
        System.out.println("Plain text password: " + password);
        System.out.println("BCrypt hash: " + hashedPassword);
        System.out.println("==================================================");
        System.out.println("\nSQL UPDATE statement:");
        System.out.println(
                "UPDATE users SET password = '" + hashedPassword + "' WHERE email = 'prof.rania.hassan@alquds.edu';");
        System.out.println("==================================================");

        // Verify it works
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("\nVerification: " + (matches ? "✓ Hash is correct" : "✗ Hash failed"));
    }
}
