## Report Page Tests (Backend + Frontend)

### Backend (Spring Boot / JUnit)
- [x] **Overview endpoint returns data**: `GET /api/admin/reports/overview?semesterId=...` returns `success=true` and a `SystemWideReport`.
- [x] **Department filter works**: `GET /api/admin/reports/overview?semesterId=...&departmentId=...` returns a report containing only that department (Admin/Deanship only).
- [ ] **Missing semesterId**: verify `GET /api/admin/reports/overview` returns `400` with error message.
- [ ] **Role restrictions**: verify non-admin/non-deanship users get `403` (if endpoint is protected by Spring Security config).
- [ ] **CSV/PDF export respects filter (optional)**:
  - `GET /api/admin/reports/export/csv?semesterId=...` (and add `departmentId` support if desired)
  - `GET /api/admin/reports/export/pdf?semesterId=...` (and add `departmentId` support if desired)

### Frontend (Jest / JSDOM)
- [x] **Filter options render**: departments + academic years populate dropdowns.
- [x] **Department dropdown triggers filtered request**: selecting a department includes `departmentId` in the `/admin/reports/overview` request.
- [ ] **Semester required UX**: if semester not selected, department change should not trigger request (toast warning is shown only when user tries to load report).
- [ ] **Academic year → semesters cascade**: selecting year triggers `/deanship/academic-years/{id}/semesters` and populates semester dropdown.

### How to Run

- **Frontend tests**:

```bash
npm test
```

- **Backend tests** (PowerShell):

```powershell
./mvnw test
```

