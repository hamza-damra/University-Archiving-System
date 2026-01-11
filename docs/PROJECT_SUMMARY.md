# Al-Quds University Archiving System - Project Summary

## Overview
The **Al-Quds University Archiving System** is a comprehensive digital platform designed to streamline and modernize the academic document management and archiving process. Built with **Spring Boot** and **MySQL**, it provides a secure, hierarchical, and semester-based structure for storing and retrieving essential academic materials.

## Key Objectives
- **Centralized Archiving**: Provide a single source of truth for all academic documents.
- **Hierarchical Access**: implement a robust role-based access control (RBAC) system.
- **Academic Context**: Organize documents by Academic Year, Semester, and Course.
- **Multi-File Support**: Handle large and diverse sets of document types.
- **Reporting & Analysis**: Provide insights into submission progress and academic activity.

## Core Features
- **Semester-Based Management**: Dynamic creation of academic years and semesters.
- **Automated Workflow**: Track document submissions from upload to verification.
- **Hierarchical File Explorer**: Intuitive file browsing based on role permissions.
- **Reporting Dashboard**: Visual representations of data for administrators and deans.
- **Security**: JWT-based authentication and secure session management.

## Technical Stack
- **Backend**: Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA.
- **Database**: MySQL 8.0 with Flyway migrations.
- **Frontend**: HTML5, Tailwind CSS, JavaScript (Vanilla ES6+).
- **Session Management**: Spring Session JDBC.

## Project Structure
- `src/main/java`: Backend logic organized by controller, service, and repository.
- `src/main/resources/static`: Web frontend partitioned by role (Admin, Deanship, HOD, Professor).
- `src/main/resources/db/migration`: Versioned database schema changes.
- `uploads/`: Physical storage for archived documents organized by academic year.

---
*Created on 2026-01-11*
