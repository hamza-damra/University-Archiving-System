# API Requests for IntelliJ IDEA

This folder contains `.http` files that can be used with the built-in HTTP Client in IntelliJ IDEA to test the REST API.

## How to use

1. Open any `.http` file in IntelliJ IDEA.
2. Ensure the application is running on `http://localhost:8080`.
3. Start by running the "Login" request in `auth.http`.
    - This will automatically set the `auth_token` and `refresh_token` variables globally for the session.
4. You can then run other requests in any file. They use the `{{auth_token}}` variable for authentication.

## Files

- `auth.http`: Authentication, token refresh, and user info.
- `admin.http`: Administrative CRUD operations for Users, Departments, Courses, and Assignments.
- `deanship.http`: Academic years, semesters, and system-wide reports.
- `hod.http`: HOD-specific operations, professor management within department, and department reports.
- `professor.http`: Professor-specific operations, course list, and submissions.
- `file-operations.http`: General file upload, explorer, and preview operations.

## Variables

The following variables are defined at the top of the files or set during login:

- `host`: The base URL of the server (default: `http://localhost:8080`).
- `auth_token`: The JWT access token (set automatically after login).
- `refresh_token`: The JWT refresh token (set automatically after login).
