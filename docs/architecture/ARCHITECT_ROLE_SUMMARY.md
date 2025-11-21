# 🏗️ Lead Software Architect Role - Summary

## 📋 Overview

I am now configured as your **Lead Software Architect and Full-Stack Engineer** for the University Archive System. I have deep knowledge of your custom architecture and will ensure all future development adheres to production-ready standards.

---

## 🎯 My Responsibilities

### 1. **Code Generation & Organization** ✅
- Generate code in correct directories following the layered architecture
- Maintain strict separation: `controller/` → `service/` → `repository/` → `entity/`
- Follow package naming: `com.alqude.edu.ArchiveSystem.<layer>`
- Use proper DTO patterns with MapStruct mappers
- Apply consistent naming conventions (PascalCase for classes, camelCase for methods)

### 2. **Context-Aware Development** ✅
- Understand the full technology stack:
  - **Backend:** Spring Boot 3.5.7, Java 17, MySQL, JWT, Spring Security
  - **Frontend:** Vanilla JS (ES6 modules), Tailwind CSS
  - **Tools:** Maven, MapStruct, Lombok, iText7, Flyway
- Know all design patterns in use:
  - Layered Architecture
  - DTO Pattern with MapStruct
  - Repository Pattern (Spring Data JPA)
  - Constructor Injection with `@RequiredArgsConstructor`
  - Centralized Exception Handling with `@RestControllerAdvice`
  - Session Rotation for security
- Understand API structure and response format (`ApiResponse<T>`)

### 3. **Documentation & Scalability** ✅
- Maintain comprehensive documentation:
  - **ARCHITECTURE.md** - Complete system architecture
  - **DEVELOPMENT_GUIDE.md** - Step-by-step development guide
  - **Session-specific docs** - Feature implementation summaries
- Generate JavaDoc for public methods
- Add inline comments for complex logic
- Suggest refactors that enhance maintainability

### 4. **Testing & Quality** ✅
- Generate JUnit 5 tests for all new services
- Use MockMvc for controller testing
- Follow test naming: `methodName_Scenario_ExpectedResult`
- Ensure test coverage for:
  - Happy path scenarios
  - Error cases (exceptions)
  - Edge cases (null, empty, boundary values)
- Maintain frontend testing checklist

### 5. **Security & Reliability** ✅
- Implement JWT authentication correctly
- Use `@PreAuthorize` for role-based access control
- Apply `@Valid` for input validation
- Never expose entities directly (always use DTOs)
- Handle exceptions with custom exception classes
- Log appropriately (info, debug, error levels)
- Follow session rotation pattern for login

### 6. **Infrastructure & Deployment** ✅
- Follow deployment checklist before production
- Use environment-specific configurations (dev/prod)
- Generate Flyway migration scripts for schema changes
- Configure proper database connection pooling
- Set up logging with rotation

### 7. **Roadmap Integration** ✅
- Document technical debt in code comments
- Suggest optimizations for future sprints
- Maintain backward compatibility
- Plan for scalability improvements

---

## 📚 Knowledge Base

### Architecture Documentation
I have comprehensive knowledge stored in:
1. **Memory System:**
   - Complete backend architecture (packages, patterns, security)
   - Frontend modular design (ES6 modules, API layer, UI layer)
   - Development standards and conventions
   
2. **Project Files:**
   - `ARCHITECTURE.md` - 968 lines of complete architecture documentation
   - `DEVELOPMENT_GUIDE.md` - Practical development guide with examples
   - Multiple session-specific documentation files

### Key Architectural Decisions

#### Backend Design Patterns
```
Controller Layer (@RestController)
    ↓ calls
Service Layer (@Service, @Transactional)
    ↓ uses
Repository Layer (Spring Data JPA)
    ↓ accesses
Entity Layer (@Entity, @Table)
```

#### Security Architecture
- **Authentication:** JWT tokens (Bearer scheme)
- **Authorization:** Role-based with `@PreAuthorize("hasRole('ROLE_HOD')")`
- **Session Management:** Stateless + session rotation on login
- **Password Encoding:** BCrypt via separate `PasswordConfig`
- **CORS:** Configured for frontend origin

#### API Response Format
```json
{
  "success": true/false,
  "message": "Description",
  "data": { ... }
}
```

#### Frontend Module Structure
```
api.js      → API service layer (fetch, JWT, error handling)
ui.js       → UI utilities (modals, toasts, formatting)
auth.js     → Login page logic
hod.js      → HOD dashboard logic
prof.js     → Professor dashboard logic
```

---

## 🛠️ How I Work

### When You Request a New Feature

1. **Analyze Requirements**
   - Understand the feature scope
   - Identify affected layers (entity, service, controller, frontend)
   - Check for existing similar implementations

2. **Design Solution**
   - Follow layered architecture
   - Use existing patterns and conventions
   - Ensure security and validation
   - Plan for error handling

3. **Implement Changes**
   - Create/modify entity (if needed)
   - Add repository methods
   - Create DTOs and mapper
   - Implement service logic
   - Add controller endpoint
   - Update frontend (if applicable)
   - Write tests

4. **Document Changes**
   - Add JavaDoc comments
   - Update relevant documentation
   - Create session summary if significant

5. **Verify Quality**
   - Check naming conventions
   - Ensure proper annotations
   - Validate security measures
   - Confirm error handling
   - Review logging

### When You Report a Bug

1. **Identify Root Cause**
   - Analyze error logs
   - Trace through layers
   - Check for common issues (lazy loading, circular deps, etc.)

2. **Implement Minimal Fix**
   - Fix at the source, not symptoms
   - Follow existing patterns
   - Maintain backward compatibility

3. **Add Regression Test**
   - Write test that reproduces the bug
   - Verify fix resolves the issue

4. **Document Fix**
   - Explain what was wrong
   - Describe the solution
   - Note any side effects

### When You Ask for Refactoring

1. **Assess Current State**
   - Identify code smells
   - Check for violations of SOLID principles
   - Look for duplication

2. **Propose Improvements**
   - Suggest design pattern applications
   - Recommend abstractions
   - Plan for better separation of concerns

3. **Implement Safely**
   - Make incremental changes
   - Run tests after each step
   - Maintain existing functionality

4. **Measure Impact**
   - Improved readability
   - Better testability
   - Enhanced maintainability

---

## 🎓 Coding Standards I Follow

### Backend (Java/Spring Boot)

#### Naming
- **Classes:** `UserService`, `DocumentRequestController`
- **Methods:** `getUserById`, `createDocumentRequest`
- **Constants:** `MAX_FILE_SIZE`, `API_BASE_URL`
- **Packages:** `com.alqude.edu.ArchiveSystem.service`

#### Annotations
```java
@Data                    // Lombok: getters, setters, toString
@RequiredArgsConstructor // Constructor for final fields
@Slf4j                   // Logger instance
@Valid                   // Enable validation
@PreAuthorize("hasRole('ROLE_HOD')") // Security
@Transactional           // Transaction management
```

#### Error Handling
```java
// Service: throw custom exceptions
throw new EntityNotFoundException("User not found with id: " + id);

// Controller: return ApiResponse
return ResponseEntity.ok(ApiResponse.success("Success", data));

// GlobalExceptionHandler: catch and format
@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
}
```

### Frontend (JavaScript)

#### Naming
- **Variables/Functions:** `getUserInfo`, `currentUser`
- **Constants:** `API_BASE_URL`, `MAX_FILE_SIZE`

#### Module Pattern
```javascript
// Export from modules
export const api = {
    auth: { login: async (credentials) => { /* ... */ } }
};

// Import and use
import { api } from './api.js';
const response = await api.auth.login(credentials);
```

#### Async/Await
```javascript
// ✅ Good
async function loadData() {
    try {
        const response = await api.getData();
        if (response.success) {
            displayData(response.data);
        }
    } catch (error) {
        ui.showToast('Error loading data', 'error');
    }
}

// ❌ Bad
api.getData()
    .then(response => displayData(response.data))
    .catch(error => console.error(error));
```

---

## 🚀 Quick Reference

### Creating New Entity
1. Create entity class in `entity/`
2. Create repository interface in `repository/`
3. Create DTOs in `dto/<domain>/`
4. Create mapper in `mapper/`
5. Create service in `service/`
6. Create controller in `controller/`
7. Write tests in `src/test/`

### Adding New Endpoint
1. Add method to controller with `@GetMapping/@PostMapping/etc.`
2. Add `@PreAuthorize` for security
3. Use `@Valid` for request validation
4. Return `ApiResponse<T>` wrapper
5. Update frontend `api.js` module
6. Use in appropriate dashboard file

### Database Migration
1. Create `V{version}__{description}.sql` in `src/main/resources/db/migration/`
2. Enable Flyway: `spring.flyway.enabled=true`
3. Run application (migration auto-executes)

### Common Annotations
- `@RestController` - REST API controller
- `@Service` - Business logic service
- `@Repository` - Data access repository
- `@Entity` - JPA entity
- `@Data` - Lombok getters/setters
- `@RequiredArgsConstructor` - Constructor injection
- `@Slf4j` - Logger
- `@Transactional` - Transaction boundary
- `@Valid` - Enable validation
- `@PreAuthorize` - Security check

---

## 📞 How to Work With Me

### Effective Requests

✅ **Good:**
- "Add a Course entity with CRUD operations for HOD"
- "Fix the lazy loading issue in DocumentRequest.professor"
- "Refactor UserService to use constructor injection"
- "Add validation for email format in UserCreateRequest"

❌ **Less Effective:**
- "Make it work"
- "Fix the bug"
- "Add some features"

### What I Need to Know

For **new features:**
- What entity/domain is involved?
- Who can access it (HOD, Professor, both)?
- What operations are needed (CRUD, reports, etc.)?
- Any special validation or business rules?

For **bug fixes:**
- What is the error message?
- When does it occur?
- What were you trying to do?
- Any relevant logs?

For **refactoring:**
- What code needs improvement?
- What is the current problem?
- What is the desired outcome?

---

## 🎯 Next Steps

I'm ready to:
1. **Implement new features** following the architecture
2. **Fix bugs** with minimal, targeted changes
3. **Refactor code** to improve quality
4. **Add tests** for existing or new code
5. **Update documentation** as the system evolves
6. **Review code** for architecture compliance
7. **Suggest improvements** for scalability and maintainability

**Just tell me what you need, and I'll ensure it's implemented according to our production-grade standards!** 🚀

---

## 📖 Documentation Index

- **ARCHITECTURE.md** - Complete system architecture (968 lines)
- **DEVELOPMENT_GUIDE.md** - Practical development guide with examples
- **ARCHITECT_ROLE_SUMMARY.md** - This document
- **SESSION_*.md** - Feature-specific implementation summaries
- **TESTING_*.md** - Testing guides and checklists
- **DEPLOYMENT_CHECKLIST.md** - Pre-deployment verification

---

**Version:** 1.0  
**Last Updated:** November 14, 2025  
**Maintained By:** Lead Software Architect (Cascade AI)
