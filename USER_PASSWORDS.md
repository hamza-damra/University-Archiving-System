# User Passwords Reference

This document lists all default user passwords created by the system initialization classes.

**Note:** Passwords are not stored in migration files. They are defined in Java initialization classes:
- `AdminInitializer.java` - Creates admin user
- `SampleDataInitializer.java` - Creates sample users (Deanship, HODs, Professors)

---

## Admin User

| Email | Password | Role | Name |
|-------|----------|------|------|
| admin@alquds.edu | **Admin@123** | Admin | System Administrator |

**Source:** `AdminInitializer.java` (line 32)

---

## Deanship User

| Email | Password | Role | Name |
|-------|----------|------|------|
| deanship@alquds.edu | **Deanship@123** | Dean | Deanship Administrator |

**Source:** `SampleDataInitializer.java` (line 189)

---

## HOD (Head of Department) Users

All HOD users share the same password:

| Email | Password | Role | Name | Department |
|-------|----------|------|------|------------|
| hod.cs@alquds.edu | **Hod@123** | HOD | Ahmed Ali | Computer Science |
| hod.math@alquds.edu | **Hod@123** | HOD | Fatima Hassan | Mathematics |
| hod.physics@alquds.edu | **Hod@123** | HOD | Mohammed Ibrahim | Physics |

**Source:** `SampleDataInitializer.java` (line 217)

---

## Professor Users

All Professor users share the same password:

| Email | Password | Role | Name | Department |
|-------|----------|------|------|------------|
| prof1@alquds.edu | **Prof@123** | Professor | Omar Khalil | Computer Science |
| prof2@alquds.edu | **Prof@123** | Professor | Layla Mahmoud | Computer Science |
| prof3@alquds.edu | **Prof@123** | Professor | Youssef Nasser | Mathematics |
| prof4@alquds.edu | **Prof@123** | Professor | Nour Salem | Mathematics |
| prof5@alquds.edu | **Prof@123** | Professor | Khalid Omar | Physics |
| prof6@alquds.edu | **Prof@123** | Professor | Sara Ahmed | Computer Science |
| prof7@alquds.edu | **Prof@123** | Professor | Hassan Mohammed | Chemistry |
| prof8@alquds.edu | **Prof@123** | Professor | Mariam Ali | Engineering |

**Source:** `SampleDataInitializer.java` (line 257)

---

## Summary by Password

- **Admin@123** - Admin user only
- **Deanship@123** - Deanship user only
- **Hod@123** - All HOD users (3 users)
- **Prof@123** - All Professor users (8 users)

---

## Important Notes

1. **Security Warning:** These are default passwords for development/testing. **Change all passwords in production!**

2. **Password Encoding:** Passwords are encoded using Spring Security's `PasswordEncoder` before being stored in the database.

3. **Initialization:** Users are created automatically when the application starts if they don't already exist.

4. **Logging:** The application logs these credentials during initialization (check application logs for confirmation).
