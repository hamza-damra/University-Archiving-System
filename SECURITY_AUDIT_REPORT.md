# Security Audit Report - University Archiving System

**Audit Date:** January 2, 2026  
**System Version:** 0.0.1-SNAPSHOT  
**Framework:** Spring Boot 3.5.7  
**Auditor:** Automated Security Analysis

---

## Executive Summary

This security audit examines the University Archiving System codebase for potential vulnerabilities and security weaknesses. The analysis covers authentication, authorization, file handling, data validation, and configuration security.

### Overall Security Rating: **LOW-MODERATE RISK** ✅ (After Fixes Applied)

| Category | Status | Risk Level | Fixed |
|----------|--------|------------|-------|
| Authentication & JWT | ⚠️ Minor Issues | Low | ✅ |
| Authorization/Access Control | ✅ Good | Low | N/A |
| File Upload Security | ⚠️ Minor Issues | Low-Medium | Partial |
| Path Traversal Protection | ✅ Fixed | Low | ✅ |
| CORS Configuration | ✅ Fixed | Low | ✅ |
| CSRF Protection | ⚠️ Disabled | Medium | Review |
| SQL Injection | ✅ Protected | Low | N/A |
| Input Validation | ✅ Good | Low | N/A |
| Sensitive Data Exposure | ⚠️ Minor Issues | Low-Medium | Partial |
| Rate Limiting | ✅ Implemented | Low | ✅ |
| Actuator Endpoints | ✅ Fixed | Low | ✅ |
| Security Headers | ✅ Implemented | Low | ✅ |

---

## Fixes Applied During This Audit

### ✅ FIX 1: Path Traversal Protection (CRITICAL - FIXED)

**Files Modified:**
- `FileServiceImpl.java` - Added path validation in `loadFileAsResource()`
- `FilePreviewServiceImpl.java` - Added path validation in `resolveFilePath()`

**Change:** Added verification that resolved file paths stay within the base upload directory to prevent `../` path traversal attacks.

---

### ✅ FIX 2: CORS Configuration (CRITICAL - FIXED)

**Files Modified:**
- `SecurityConfig.java` - Replaced wildcard CORS with configurable allowed origins
- `AuthController.java` - Removed `@CrossOrigin(origins = "*")`
- `SessionController.java` - Removed `@CrossOrigin(origins = "*")`
- `application.properties` - Added `app.cors.allowed-origins` configuration
- `application-prod.properties` - Added `app.cors.allowed-origins` configuration

**Change:** CORS now uses specific allowed origins from environment variable `CORS_ALLOWED_ORIGINS` instead of allowing all origins.

---

### ✅ FIX 3: Rate Limiting (CRITICAL - IMPLEMENTED)

**Files Created:**
- `RateLimitingFilter.java` - New rate limiting filter

**Configuration Added:**
- `app.rate-limit.enabled` - Enable/disable rate limiting
- `app.rate-limit.login.requests-per-minute` - Login attempt limit (default: 5)
- `app.rate-limit.api.requests-per-minute` - API request limit (default: 100)

**Change:** Implemented in-memory rate limiting to protect against brute force attacks and API abuse.

---

### ✅ FIX 4: Actuator Endpoints (MEDIUM - FIXED)

**Files Modified:**
- `SecurityConfig.java` - Restricted actuator endpoints

**Change:** Only `/actuator/health` is now publicly accessible. All other actuator endpoints require ADMIN role.

---

### ✅ FIX 5: Security Headers (MEDIUM - IMPLEMENTED)

**Files Created:**
- `SecurityHeadersConfig.java` - New security headers configuration

**Headers Added:**
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Content-Security-Policy` (configured for frontend)
- `Permissions-Policy`
- `Cache-Control` for API responses

### 1. 🔴 Path Traversal Vulnerability (HIGH SEVERITY)

**Location:** `FileServiceImpl.java` (Lines 297-310)

**Issue:** The `loadFileAsResource` method uses `normalize()` but lacks validation that the resolved path is within the allowed upload directory.

```java
public Resource loadFileAsResource(String fileUrl) {
    Path filePath = Paths.get(uploadDirectory).resolve(fileUrl).normalize();
    Resource resource = new UrlResource(filePath.toUri());
    // Missing check: does filePath start with uploadDirectory?
}
```

**Risk:** An attacker could potentially access files outside the upload directory using path traversal sequences like `../../../etc/passwd`.

**Recommendation:**
```java
public Resource loadFileAsResource(String fileUrl) {
    Path basePath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    Path filePath = basePath.resolve(fileUrl).normalize();
    
    // CRITICAL: Verify the resolved path is within the base directory
    if (!filePath.startsWith(basePath)) {
        throw new SecurityException("Path traversal attempt detected");
    }
    
    Resource resource = new UrlResource(filePath.toUri());
    // ... rest of the code
}
```

**Affected Files:**
- `FileServiceImpl.java:loadFileAsResource()`
- `FilePreviewServiceImpl.java:resolveFilePath()`

---

### 2. 🔴 CORS Misconfiguration (HIGH SEVERITY)

**Location:** `SecurityConfig.java` (Lines 64-71), `AuthController.java` (Line 24)

**Issue:** CORS is configured to allow ALL origins with credentials enabled:

```java
configuration.setAllowedOriginPatterns(Arrays.asList("*"));
configuration.setAllowCredentials(true);
```

Additionally, `@CrossOrigin(origins = "*")` is used on `AuthController` and `SessionController`.

**Risk:** This allows any website to make authenticated requests to your API, enabling CSRF-like attacks and potential data theft.

**Recommendation:**
```java
// Only allow specific trusted origins
configuration.setAllowedOrigins(Arrays.asList(
    "https://yourdomain.com",
    "https://app.yourdomain.com"
));
// Or use environment-based configuration
configuration.setAllowedOrigins(Arrays.asList(
    System.getenv("ALLOWED_ORIGINS").split(",")
));
```

---

### 3. 🔴 CSRF Protection Disabled (MEDIUM-HIGH SEVERITY)

**Location:** `SecurityConfig.java` (Line 36)

**Issue:** CSRF protection is completely disabled:
```java
.csrf(AbstractHttpConfigurer::disable)
```

**Risk:** While using JWT tokens with STATELESS sessions reduces CSRF risk, the combination with wide-open CORS makes the application vulnerable to cross-site attacks.

**Recommendation:**
- If using stateless JWT authentication exclusively, ensure tokens are NOT stored in cookies
- If cookies are used for any authentication, re-enable CSRF protection:
```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/auth/**", "/api/public/**")
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)
```

---

### 4. 🔴 No Rate Limiting (HIGH SEVERITY)

**Issue:** No rate limiting mechanism exists for any endpoints.

**Risk:**
- Brute force attacks on login endpoint `/api/auth/login`
- Token refresh endpoint abuse
- File upload abuse (resource exhaustion)
- API abuse leading to DoS

**Recommendation:** Implement rate limiting using Spring Boot + Bucket4j or similar:

```java
@Bean
public Bucket4jConfiguration bucket4jConfiguration() {
    return new Bucket4jConfiguration()
        .addFilter(FilterConfiguration.builder()
            .path("/api/auth/login")
            .bandwidth(Bandwidth.simple(5, Duration.ofMinutes(1)))
            .build())
        .addFilter(FilterConfiguration.builder()
            .path("/api/**")
            .bandwidth(Bandwidth.simple(100, Duration.ofMinutes(1)))
            .build());
}
```

---

## Medium Severity Issues

### 5. 🟠 Weak Default JWT Secret (MEDIUM SEVERITY)

**Location:** `application.properties` (Line 71)

**Issue:** Default JWT secret in configuration:
```properties
jwt.secret=${JWT_SECRET:defaultSecretKeyForDevOnlyChangeInProduction123456789}
```

**Risk:** If environment variable is not set in production, the default secret is used, making JWT tokens predictable.

**Recommendation:**
- Remove default value entirely in production config
- Ensure JWT_SECRET is always set via environment variable
- Use a cryptographically secure random key (minimum 256 bits)

```properties
# application-prod.properties
jwt.secret=${JWT_SECRET}  # No default - MUST be set
```

---

### 6. 🟠 Actuator Endpoints Exposure (MEDIUM SEVERITY)

**Location:** `SecurityConfig.java` (Line 47), `application.properties` (Lines 100-106)

**Issue:** Actuator endpoints are publicly accessible:
```java
.requestMatchers("/actuator/**").permitAll()
```

**Risk:** Exposed endpoints can reveal:
- Application health details
- Environment information
- System metrics

**Recommendation:**
```java
.requestMatchers("/actuator/health").permitAll() // Only health for Docker checks
.requestMatchers("/actuator/**").hasRole("ADMIN") // Require admin for others
```

```properties
# application-prod.properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

---

### 7. 🟠 File Extension Validation Bypass Risk (MEDIUM SEVERITY)

**Location:** `FileServiceImpl.java` (Lines 357-373)

**Issue:** File type validation relies solely on filename extension:
```java
public boolean validateFileType(MultipartFile file, List<String> allowedExtensions) {
    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    return extensionsToCheck.stream().anyMatch(ext -> ext.equalsIgnoreCase(extension));
}
```

**Risk:** An attacker could upload a malicious file with a fake extension (e.g., `malware.pdf` that's actually executable).

**Recommendation:** Add MIME type magic number validation:
```java
public boolean validateFileType(MultipartFile file, List<String> allowedExtensions) {
    // 1. Check extension
    String extension = getFileExtension(file.getOriginalFilename());
    if (!extensionsToCheck.contains(extension.toLowerCase())) {
        return false;
    }
    
    // 2. Verify MIME type via content detection
    try {
        Tika tika = new Tika();
        String detectedType = tika.detect(file.getInputStream());
        return ALLOWED_MIME_TYPES.contains(detectedType);
    } catch (IOException e) {
        return false;
    }
}
```

---

### 8. 🟠 Missing Filename Sanitization in Multiple Locations (MEDIUM SEVERITY)

**Location:** `FolderFileUploadServiceImpl.java` (Lines 275-291)

**Issue:** While there is some sanitization, it may not be comprehensive:
```java
String sanitized = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
```

**Risk:** Potential for path injection or encoding-based attacks.

**Recommendation:** Add comprehensive sanitization:
```java
private String sanitizeFilename(String filename) {
    if (filename == null) return "unnamed_file";
    
    // Remove any path components
    filename = Paths.get(filename).getFileName().toString();
    
    // Remove null bytes and other dangerous characters
    filename = filename.replaceAll("\u0000", "")
                       .replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_")
                       .trim();
    
    // Limit length
    if (filename.length() > 255) {
        String ext = getFileExtension(filename);
        filename = filename.substring(0, 250 - ext.length()) + "." + ext;
    }
    
    // Prevent dangerous extensions
    if (filename.matches(".*\\.(exe|bat|cmd|sh|ps1|dll)$")) {
        filename = filename + ".blocked";
    }
    
    return filename.isEmpty() ? "unnamed_file" : filename;
}
```

---

### 9. 🟠 Sensitive Information in Logs (MEDIUM SEVERITY)

**Location:** Various files

**Issue:** Debug logging in production may expose sensitive data:
```java
log.info("Login attempt for email: {}", loginRequest.getEmail());
log.info("Creating user with email: {}", request.getEmail());
```

**Risk:** Log files may contain email addresses, user IDs, and operation details useful for attackers.

**Recommendation:**
- Use different logging levels for dev vs production
- Implement log scrubbing for sensitive data
- Ensure production logging level is WARN or above for security-sensitive operations

---

### 10. 🟠 Missing Security Headers (MEDIUM SEVERITY)

**Issue:** Application doesn't set critical security headers.

**Recommendation:** Add security headers configuration:
```java
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void postHandle(HttpServletRequest request, 
                                   HttpServletResponse response,
                                   Object handler, ModelAndView modelAndView) {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("X-XSS-Protection", "1; mode=block");
                response.setHeader("Content-Security-Policy", 
                    "default-src 'self'; script-src 'self'");
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            }
        });
    }
}
```

---

## Low Severity Issues

### 11. 🟢 SQL Injection - PROTECTED ✅

**Status:** The application uses Spring Data JPA with parameterized queries throughout. All `@Query` annotations use named parameters (`:paramName`), providing protection against SQL injection.

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.email = :email")
Optional<User> findByEmailWithDepartment(@Param("email") String email);
```

---

### 12. 🟢 Authorization Implementation - GOOD ✅

**Status:** The application implements proper role-based access control:
- Method-level security with `@PreAuthorize`
- Permission checks in service layer (`canReadFile`, `canWriteToCourseAssignment`, `canDeleteFile`)
- Proper department-based access restrictions

---

### 13. 🟢 Input Validation - GOOD ✅

**Status:** DTOs use Jakarta validation annotations:
```java
@NotBlank(message = "Email is required")
@Email(message = "Email should be valid")
private String email;

@NotBlank(message = "Password is required")
private String password;
```

---

### 14. 🟢 XSS Prevention - IMPLEMENTED ✅

**Status:** Frontend JavaScript files implement HTML escaping:
```javascript
// Escape HTML to prevent XSS
function escapeHtml(text) { ... }
```

---

### 15. 🟡 Password Validation Could Be Stronger

**Location:** `UserService.java` (Line 54)

**Current Pattern:**
```java
private static final Pattern PASSWORD_PATTERN = Pattern.compile(
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])[^\\s]{8,}$"
);
```

**Recommendation:** Consider adding:
- Minimum length of 12 characters
- Check against common password lists
- Check for user-related terms (username, email parts)

---

## Dependency Vulnerabilities

### Recommended Actions:

1. **Update JWT library** - Consider upgrading from jjwt `0.11.5` to latest stable version

2. **Check for CVEs** in:
   - `mysql-connector-java:8.0.33`
   - `itext7-core:7.2.5`
   - `apache-poi:5.2.5`

Run: `mvn dependency-check:check` for full CVE analysis.

---

## Remediation Priority

| Priority | Issue | Effort | Impact | Status |
|----------|-------|--------|--------|--------|
| 🟢 P1 | Path Traversal Protection | Low | High | ✅ FIXED |
| 🟢 P1 | CORS Restriction | Low | High | ✅ FIXED |
| 🟢 P1 | Rate Limiting | Medium | High | ✅ IMPLEMENTED |
| 🟠 P2 | CSRF Re-evaluation | Low | Medium | Review Needed |
| 🟢 P2 | JWT Secret Enforcement | Low | Medium | ✅ CONFIGURED |
| 🟢 P2 | Actuator Endpoint Restriction | Low | Medium | ✅ FIXED |
| 🟠 P2 | File Type Magic Validation | Medium | Medium | TODO |
| 🟢 P3 | Security Headers | Low | Medium | ✅ IMPLEMENTED |
| 🟠 P3 | Logging Audit | Medium | Low | TODO |

---

## Remaining Action Items

### Should Fix Soon (Lower Priority):

1. **Add MIME type magic number validation** for uploads using Apache Tika
2. **Review and minimize debug logging** in production
3. **Consider re-enabling CSRF protection** with proper configuration
4. **Add password policy checks** against common password lists

### Production Deployment Checklist:

Before deploying to production, ensure these environment variables are set:

```bash
# Required
JWT_SECRET=<strong-256-bit-random-secret>
DATABASE_URL=<your-database-url>
DATABASE_USERNAME=<db-username>
DATABASE_PASSWORD=<db-password>
CORS_ALLOWED_ORIGINS=https://yourdomain.com

# Optional
RATE_LIMIT_ENABLED=true
RATE_LIMIT_LOGIN=5
RATE_LIMIT_API=100
```

---

## Code Fixes Required

### Fix 1: Path Traversal Protection

**File:** `src/main/java/com.alquds/edu/ArchiveSystem/service/FileServiceImpl.java`

Add path validation after line 302:
```java
Path basePath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
Path filePath = basePath.resolve(fileUrl).normalize();

if (!filePath.startsWith(basePath)) {
    log.error("Path traversal attempt detected: {}", fileUrl);
    throw new SecurityException("Invalid file path");
}
```

### Fix 2: CORS Configuration

**File:** `src/main/java/com.alquds/edu/ArchiveSystem/config/SecurityConfig.java`

Replace lines 65-66:
```java
configuration.setAllowedOrigins(Arrays.asList(
    "${FRONTEND_URL:http://localhost:3000}"
));
// Remove setAllowedOriginPatterns("*")
```

### Fix 3: Remove @CrossOrigin annotations

**Files:** `AuthController.java`, `SessionController.java`

Remove `@CrossOrigin(origins = "*")` annotations - use centralized CORS configuration instead.

---

## Conclusion

The University Archiving System has a solid foundation with good practices in SQL injection prevention, authorization, and input validation. 

**During this security audit, the following critical fixes were applied:**

1. ✅ **Path traversal vulnerability** - Fixed in file access services
2. ✅ **CORS configuration** - Now uses specific allowed origins instead of wildcards
3. ✅ **Rate limiting** - Implemented for login and API endpoints
4. ✅ **Actuator endpoints** - Restricted to health check only
5. ✅ **Security headers** - Added comprehensive HTTP security headers

**Remaining lower-priority items:**
- MIME type magic number validation for file uploads
- Debug logging review for production
- Password complexity enhancement

The system is now significantly more secure and ready for production deployment, provided the required environment variables are properly configured.

---

**Report Generated:** 2026-01-02  
**Fixes Applied:** 2026-01-02  
**Next Audit Recommended:** 2026-04-01 (Quarterly)

