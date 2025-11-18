# Mock Accounts Documentation

## ⚠️ SECURITY WARNING

**NEVER use these accounts in production!**

All mock accounts use a default password for testing purposes only:
- **Default Password:** `password123`

These accounts are intended for development and testing environments only. Before deploying to production:
- Delete all mock accounts or change passwords to strong, unique values
- Disable mock data generation by setting `mock.data.enabled=false`
- Use proper authentication and password policies

---

## Dean Accounts

Dean accounts have the highest administrative privileges to view all departments and generate university-wide reports.

| Email | Name | Role |
|-------|------|------|
| dean@alquds.edu | Dean User | ROLE_DEAN |

**Capabilities:**
- View all departments and their professors
- Generate university-wide submission reports
- Access cross-department analytics
- View all course assignments across all departments
- Monitor submission compliance across the entire university

---

## HOD (Head of Department) Accounts

HOD accounts have administrative privileges to manage professors and view department-wide reports.

| Email | Department | Name (Generated) |
|-------|------------|------------------|
| hod.cs@alquds.edu | Computer Science | *Randomly generated* |
| hod.math@alquds.edu | Mathematics | *Randomly generated* |
| hod.phys@alquds.edu | Physics | *Randomly generated* |
| hod.eng@alquds.edu | Engineering | *Randomly generated* |
| hod.bus@alquds.edu | Business Administration | *Randomly generated* |

**Note:** Names are randomly generated from a pool of realistic Arabic names. The exact names will vary each time mock data is created.

---

## Professor Accounts

Professor accounts can view their course assignments, submit documents, and receive notifications.

### Account Structure

- **Total Professors:** 25 (5 per department)
- **Email Format:** `prof.{firstname}.{lastname}@alquds.edu`
- **Professor ID Format:** `P{DEPT}{###}` (e.g., PCS001, PMATH002)
- **Status Distribution:** 80% Active, 20% Inactive (for testing filters)

### Computer Science Department (CS)

| Professor ID | Email Pattern | Status | Department |
|--------------|---------------|--------|------------|
| PCS001 | prof.{name}.{name}@alquds.edu | Active | Computer Science |
| PCS002 | prof.{name}.{name}@alquds.edu | Active | Computer Science |
| PCS003 | prof.{name}.{name}@alquds.edu | Active | Computer Science |
| PCS004 | prof.{name}.{name}@alquds.edu | Active | Computer Science |
| PCS005 | prof.{name}.{name}@alquds.edu | Inactive | Computer Science |

### Mathematics Department (MATH)

| Professor ID | Email Pattern | Status | Department |
|--------------|---------------|--------|------------|
| PMATH001 | prof.{name}.{name}@alquds.edu | Active | Mathematics |
| PMATH002 | prof.{name}.{name}@alquds.edu | Active | Mathematics |
| PMATH003 | prof.{name}.{name}@alquds.edu | Active | Mathematics |
| PMATH004 | prof.{name}.{name}@alquds.edu | Active | Mathematics |
| PMATH005 | prof.{name}.{name}@alquds.edu | Inactive | Mathematics |

### Physics Department (PHYS)

| Professor ID | Email Pattern | Status | Department |
|--------------|---------------|--------|------------|
| PPHYS001 | prof.{name}.{name}@alquds.edu | Active | Physics |
| PPHYS002 | prof.{name}.{name}@alquds.edu | Active | Physics |
| PPHYS003 | prof.{name}.{name}@alquds.edu | Active | Physics |
| PPHYS004 | prof.{name}.{name}@alquds.edu | Active | Physics |
| PPHYS005 | prof.{name}.{name}@alquds.edu | Inactive | Physics |

### Engineering Department (ENG)

| Professor ID | Email Pattern | Status | Department |
|--------------|---------------|--------|------------|
| PENG001 | prof.{name}.{name}@alquds.edu | Active | Engineering |
| PENG002 | prof.{name}.{name}@alquds.edu | Active | Engineering |
| PENG003 | prof.{name}.{name}@alquds.edu | Active | Engineering |
| PENG004 | prof.{name}.{name}@alquds.edu | Active | Engineering |
| PENG005 | prof.{name}.{name}@alquds.edu | Inactive | Engineering |

### Business Administration Department (BUS)

| Professor ID | Email Pattern | Status | Department |
|--------------|---------------|--------|------------|
| PBUS001 | prof.{name}.{name}@alquds.edu | Active | Business Administration |
| PBUS002 | prof.{name}.{name}@alquds.edu | Active | Business Administration |
| PBUS003 | prof.{name}.{name}@alquds.edu | Active | Business Administration |
| PBUS004 | prof.{name}.{name}@alquds.edu | Active | Business Administration |
| PBUS005 | prof.{name}.{name}@alquds.edu | Inactive | Business Administration |

**Note:** Actual email addresses contain randomly generated Arabic names (e.g., `prof.ahmad.alnajjar@alquds.edu`, `prof.fatima.almasri@alquds.edu`). Every 5th professor is set to inactive status for testing filtering functionality.

---

## Testing Scenarios

### 1. Dean Login and University-Wide Management

**Test as Dean:**
```
Email: dean@alquds.edu
Password: password123
```

**Expected Capabilities:**
- View all departments (CS, Math, Physics, Engineering, Business)
- View all professors across all departments
- Generate university-wide submission reports
- Access cross-department analytics and statistics
- Monitor compliance rates across the entire university
- View all course assignments and submissions

### 2. HOD Login and Department Management

**Test as HOD:**
```
Email: hod.cs@alquds.edu
Password: password123
```

**Expected Capabilities:**
- View all professors in the Computer Science department
- View course assignments for all CS professors
- Generate department-wide submission reports
- Filter professors by status (active/inactive)
- View submission statistics and compliance rates

### 2. Professor Login and Document Submission

**Test as Active Professor:**
```
Email: prof.{generated-name}@alquds.edu (use any PCS001-PCS004)
Password: password123
```

**Expected Capabilities:**
- View assigned courses across all semesters
- See required document types for each course
- Submit documents with file uploads
- View submission history and status
- Receive notifications about deadlines and requests

### 3. Inactive Professor Testing

**Test as Inactive Professor:**
```
Email: prof.{generated-name}@alquds.edu (use PCS005)
Password: password123
```

**Expected Behavior:**
- Login should succeed (account exists)
- May have restricted access depending on system rules
- Useful for testing status-based filtering in HOD views

### 4. Cross-Department Testing

**Test Department Isolation:**
- Login as `hod.cs@alquds.edu`
- Verify you can only see CS department professors
- Login as `hod.math@alquds.edu`
- Verify you can only see Math department professors

### 5. Multi-Semester Testing

**Test Semester-Based System:**
- Login as any professor
- View assignments across different semesters (Fall, Spring, Summer)
- Verify document requirements vary by semester
- Test submission workflows for different academic years

### 6. Notification Testing

**Test Notification System:**
- Login as any professor
- Check notification dropdown for various notification types:
  - New course assignments
  - Deadline reminders
  - Submission confirmations
  - Overdue notices
- Mark notifications as read/unread
- Verify notification counts update correctly

### 7. Report Generation Testing

**Test Reporting Features:**
- Login as HOD
- Generate department submission reports
- Filter by semester, course, or professor
- Export reports (if feature available)
- Verify data accuracy against mock data

---

## Quick Reference

### Finding Actual Email Addresses

Since names are randomly generated, you can find actual email addresses by:

1. **Check Application Logs:**
   ```
   Look for "Created HOD users" and "Created professor users" log entries
   ```

2. **Query Database:**
   ```sql
   SELECT email, firstName, lastName, role, professorId, isActive 
   FROM users 
   WHERE role = 'ROLE_PROFESSOR' 
   ORDER BY department_id, professorId;
   ```

3. **Use HOD Account:**
   - Login as any HOD
   - Navigate to professor management
   - View complete list with email addresses

### Password Reset

If you need to reset passwords for testing:

```sql
-- Reset all mock account passwords to 'password123'
UPDATE users 
SET password = '$2a$10$...' -- BCrypt hash of 'password123'
WHERE email LIKE '%@alquds.edu';
```

---

## Additional Notes

- All accounts are created with `isActive = true` except for every 5th professor
- Professor IDs follow the pattern: P{DEPARTMENT_CODE}{SEQUENCE_NUMBER}
- Names are generated from a pool of realistic Arabic names (both male and female)
- Email addresses are normalized (lowercase, no spaces, "al-" prefix removed from last names)
- All passwords are hashed using BCrypt before storage
