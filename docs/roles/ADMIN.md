# Role Documentation: Admin (ROLE_ADMIN)

## Role Scope
The **Admin** is the highest level of authority within the University Archiving System. Users with this role have unrestricted access to all system features, data, and configuration settings.

## Primary Responsibilities
1. **User Management**:
   - Create, update, and deactivate accounts for all roles (Admin, Deanship, HOD, Professor).
   - Reset passwords and manage user permissions.
   - Filter users by department, role, or activity status.
2. **System Configuration**:
   - Oversee the overall academic structure.
   - Manage global settings and system-wide parameters.
3. **Data Migration**:
   - Execute and monitor database migrations via the Migration Controller.
   - Analyze and resolve data consistency issues during system updates.
4. **Monitoring & Auditing**:
   - View high-level dashboard statistics across the entire university.
   - Monitor system health via Spring Boot Actuator endpoints.
   - Access and review security audit reports.
5. **Academic Foundation**:
   - Manage the catalog of Departments and Courses.

## Key Access Points
- **Dashboard**: Full overview of system-wide submission counts, user activity, and growth trends.
- **User Management**: Interface for managing the university's human resources.
- **Migration Panel**: Special tools for backend maintenance and data evolution.

## Limitations
- There are virtually no functional limitations for the Admin role; however, actions should be taken with care as they affect the entire system's integrity.
