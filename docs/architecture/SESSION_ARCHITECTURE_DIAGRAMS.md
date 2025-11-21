# Session Management Architecture Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Browser                          │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Cookie: ARCHIVESESSION=abc123...                   │    │
│  │  - HttpOnly: true                                   │    │
│  │  - Secure: true (prod)                              │    │
│  │  - SameSite: Strict (prod)                          │    │
│  └────────────────────────────────────────────────────┘    │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTPS
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot Application                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Spring Security Filter Chain                         │  │
│  │  ┌─────────────────────────────────────────────┐    │  │
│  │  │  SessionRepositoryFilter                     │    │  │
│  │  │  (Spring Session JDBC)                       │    │  │
│  │  └────────────────┬────────────────────────────┘    │  │
│  └───────────────────┼──────────────────────────────────┘  │
│                      ▼                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Controllers                                          │  │
│  │  - AuthController (login/logout)                     │  │
│  │  - SessionController (session management)            │  │
│  │  - HodController, ProfessorController                │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   ▼                                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Services                                             │  │
│  │  - AuthService (session rotation)                    │  │
│  │  - UserService, DocumentRequestService               │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   ▼                                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  JdbcIndexedSessionRepository                        │  │
│  │  (Spring Session)                                     │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │                                         │
│  ┌────────────────┼─────────────────────────────────────┐  │
│  │  HikariCP      ▼                                     │  │
│  │  Connection Pool                                     │  │
│  └────────────────┬─────────────────────────────────────┘  │
└───────────────────┼───────────────────────────────────────┘
                    │ JDBC
                    ▼
┌─────────────────────────────────────────────────────────────┐
│                    MySQL Database                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  SPRING_SESSION Table                                 │  │
│  │  - PRIMARY_ID (PK)                                    │  │
│  │  - SESSION_ID (UNIQUE INDEX)                          │  │
│  │  - CREATION_TIME                                      │  │
│  │  - LAST_ACCESS_TIME                                   │  │
│  │  - MAX_INACTIVE_INTERVAL                              │  │
│  │  - EXPIRY_TIME (INDEX)                                │  │
│  │  - PRINCIPAL_NAME (INDEX)                             │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  SPRING_SESSION_ATTRIBUTES Table                      │  │
│  │  - SESSION_PRIMARY_ID (FK)                            │  │
│  │  - ATTRIBUTE_NAME                                     │  │
│  │  - ATTRIBUTE_BYTES (BLOB)                             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Session Lifecycle Flow

```
┌──────────────┐
│  User Visit  │
│   Website    │
└──────┬───────┘
       │
       ▼
┌─────────────────────────┐
│ No session exists?      │
│ Create new session      │
│ SESSION_ID = random_id  │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│ Store in database:      │
│ SPRING_SESSION table    │
│ Set cookie in response  │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│ User browses site       │
│ (unauthenticated)       │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│ User clicks LOGIN       │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│ POST /api/auth/login    │
│ {email, password}       │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│ AuthService.login()             │
│ 1. Get old session              │
│ 2. Invalidate old session       │
│ 3. Authenticate user            │
│ 4. Create NEW session (new ID)  │
│ 5. Store auth in new session    │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────┐
│ Session Rotation!       │
│ OLD_ID ≠ NEW_ID         │
│ (Security feature)      │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│ User is authenticated   │
│ Browse protected pages  │
│ Each request updates    │
│ LAST_ACCESS_TIME        │
└──────┬──────────────────┘
       │
       ├──────────────────┐
       │                  │
       ▼                  ▼
┌──────────────┐   ┌──────────────┐
│ User active  │   │ Inactive for │
│ (< 30 min)   │   │ 30+ minutes  │
└──────┬───────┘   └──────┬───────┘
       │                  │
       │                  ▼
       │           ┌──────────────┐
       │           │ Session      │
       │           │ Expires      │
       │           │ (auto logout)│
       │           └──────────────┘
       │
       ▼
┌──────────────┐
│ User clicks  │
│ LOGOUT       │
└──────┬───────┘
       │
       ▼
┌─────────────────────────┐
│ POST /api/auth/logout   │
│ 1. Get session          │
│ 2. Invalidate session   │
│ 3. Delete from DB       │
│ 4. Clear cookie         │
└─────────────────────────┘
```

---

## Session Fixation Protection Flow

```
BEFORE (Vulnerable - Old Method):
┌──────────────────────────────────────────────────────┐
│ 1. Attacker creates session: SESSION_ID = AAA       │
│ 2. Attacker tricks victim to use SESSION_ID = AAA   │
│ 3. Victim logs in with SESSION_ID = AAA             │
│ 4. Session is now authenticated                      │
│ 5. ❌ Attacker can use SESSION_ID = AAA (hijacked!) │
└──────────────────────────────────────────────────────┘

AFTER (Protected - Our Method):
┌──────────────────────────────────────────────────────┐
│ 1. Attacker creates session: SESSION_ID = AAA       │
│ 2. Attacker tricks victim to use SESSION_ID = AAA   │
│ 3. Victim logs in...                                 │
│    ▼                                                  │
│    OLD session AAA is INVALIDATED                    │
│    NEW session BBB is CREATED                        │
│    ▼                                                  │
│ 4. Session BBB is authenticated                      │
│ 5. ✅ Attacker's session AAA is useless!            │
└──────────────────────────────────────────────────────┘

Implementation:
┌─────────────────────────────────────────┐
│ AuthService.login():                    │
│                                         │
│ HttpSession oldSession =                │
│     request.getSession(false);          │
│ if (oldSession != null) {               │
│     oldSession.invalidate(); // ← Key! │
│ }                                       │
│                                         │
│ // Authenticate user...                │
│                                         │
│ HttpSession newSession =                │
│     request.getSession(true); // ← New!│
│                                         │
│ // Store authentication in new session │
└─────────────────────────────────────────┘
```

---

## Horizontal Scaling Architecture

```
Multiple Application Instances (Stateless):

┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   Instance 1    │         │   Instance 2    │         │   Instance 3    │
│   :8080         │         │   :8081         │         │   :8082         │
│                 │         │                 │         │                 │
│  No in-memory   │         │  No in-memory   │         │  No in-memory   │
│  session state  │         │  session state  │         │  session state  │
└────────┬────────┘         └────────┬────────┘         └────────┬────────┘
         │                           │                           │
         └───────────────────────────┼───────────────────────────┘
                                     │
                                     ▼
                         ┌────────────────────────┐
                         │   Load Balancer        │
                         │   (No sticky sessions  │
                         │    required!)          │
                         └────────┬───────────────┘
                                  │
                                  ▼
                    ┌──────────────────────────────┐
                    │  Shared Session Store         │
                    │  (MySQL Database)             │
                    │                               │
                    │  SPRING_SESSION tables        │
                    │  - All instances read/write   │
                    │    from same location         │
                    │  - Sessions survive restarts  │
                    │  - Any instance can serve     │
                    │    any user                   │
                    └───────────────────────────────┘

Benefits:
✅ No sticky sessions needed
✅ Load balancer can route freely
✅ Survive instance crashes
✅ True horizontal scaling
✅ Zero-downtime deployments
```

---

## Database Schema Relationships

```
┌──────────────────────────────────────────────────┐
│              SPRING_SESSION                       │
├──────────────────────────────────────────────────┤
│ PRIMARY_ID (PK) ────────────────────────────┐    │
│ SESSION_ID (UNIQUE) ← Used in cookie        │    │
│ CREATION_TIME                                │    │
│ LAST_ACCESS_TIME                             │    │
│ MAX_INACTIVE_INTERVAL                        │    │
│ EXPIRY_TIME (INDEX) ← For cleanup job        │    │
│ PRINCIPAL_NAME (INDEX) ← Username            │    │
└──────────────────────────────────────────────────┘
                                                │
                                                │ 1:N
                                                │ ON DELETE CASCADE
                                                │
                                                ▼
┌──────────────────────────────────────────────────┐
│       SPRING_SESSION_ATTRIBUTES                   │
├──────────────────────────────────────────────────┤
│ SESSION_PRIMARY_ID (FK) ◄─────────────────────┘  │
│ ATTRIBUTE_NAME (PK)                              │
│ ATTRIBUTE_BYTES (BLOB)                           │
│                                                   │
│ Stores:                                          │
│ - SPRING_SECURITY_CONTEXT                        │
│ - User authentication details                    │
│ - Custom session attributes                      │
└──────────────────────────────────────────────────┘

Index Strategy:
┌─────────────────────────────────────┐
│ SPRING_SESSION_IX1 (SESSION_ID)     │ ← Fast lookup by cookie value
│ SPRING_SESSION_IX2 (EXPIRY_TIME)    │ ← Fast cleanup of expired
│ SPRING_SESSION_IX3 (PRINCIPAL_NAME) │ ← Find all user's sessions
└─────────────────────────────────────┘
```

---

## Cleanup Process Flow

```
Every 15 minutes (Cron: 0 */15 * * * *):

┌────────────────────────────────────────────┐
│  Spring Session Cleanup Task               │
└────────────┬───────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────┐
│  SELECT * FROM SPRING_SESSION              │
│  WHERE EXPIRY_TIME <= NOW()                │
└────────────┬───────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────┐
│  Found expired sessions?                   │
└────────────┬───────────────────────────────┘
             │
             ├─── NO ──► Skip
             │
             └─── YES ──►
                         │
                         ▼
             ┌──────────────────────────────┐
             │  DELETE FROM                  │
             │  SPRING_SESSION_ATTRIBUTES    │
             │  WHERE SESSION_PRIMARY_ID IN  │
             │  (expired sessions)           │
             └────────────┬─────────────────┘
                          │
                          ▼
             ┌──────────────────────────────┐
             │  DELETE FROM SPRING_SESSION   │
             │  WHERE EXPIRY_TIME <= NOW()   │
             └────────────┬─────────────────┘
                          │
                          ▼
             ┌──────────────────────────────┐
             │  Log: Cleaned X sessions      │
             └───────────────────────────────┘

Benefits:
- Prevents table bloat
- Maintains database performance
- Automatic (no manual intervention)
- Configurable schedule
```

---

## Security Layers

```
┌─────────────────────────────────────────────────────────┐
│                   Security Layers                        │
└─────────────────────────────────────────────────────────┘

Layer 1: Transport Security
┌─────────────────────────────────────────────────────────┐
│  HTTPS (TLS 1.2+)                                        │
│  - Encrypts all traffic                                  │
│  - Prevents eavesdropping                                │
│  - Required in production                                │
└─────────────────────────────────────────────────────────┘
                           ▼
Layer 2: Cookie Security
┌─────────────────────────────────────────────────────────┐
│  HttpOnly Flag                                           │
│  - JavaScript cannot access cookie                       │
│  - Prevents XSS attacks                                  │
│                                                          │
│  Secure Flag                                             │
│  - Cookie only sent over HTTPS                           │
│  - Prevents man-in-the-middle attacks                    │
│                                                          │
│  SameSite Attribute                                      │
│  - Lax: Some cross-site usage allowed                    │
│  - Strict: No cross-site usage at all                    │
│  - Prevents CSRF attacks                                 │
└─────────────────────────────────────────────────────────┘
                           ▼
Layer 3: Session Management
┌─────────────────────────────────────────────────────────┐
│  Session Fixation Protection                             │
│  - New session ID on authentication                      │
│  - Prevents session hijacking                            │
│                                                          │
│  Session Timeout                                         │
│  - Automatic logout after inactivity                     │
│  - Configurable per environment                          │
│                                                          │
│  Maximum Concurrent Sessions                             │
│  - Limit sessions per user (5)                           │
│  - Invalidate oldest if exceeded                         │
└─────────────────────────────────────────────────────────┘
                           ▼
Layer 4: Database Security
┌─────────────────────────────────────────────────────────┐
│  Database User Permissions                               │
│  - Only CRUD operations (no DDL)                         │
│  - IP-restricted access                                  │
│                                                          │
│  Connection Pool Security                                │
│  - Limited connections                                   │
│  - Connection timeout                                    │
│                                                          │
│  Data Encryption (optional)                              │
│  - At-rest encryption                                    │
│  - SSL/TLS connections                                   │
└─────────────────────────────────────────────────────────┘
                           ▼
Layer 5: Application Security
┌─────────────────────────────────────────────────────────┐
│  Spring Security                                         │
│  - Authentication & Authorization                        │
│  - Role-based access control (RBAC)                      │
│  - Method-level security                                 │
│                                                          │
│  Input Validation                                        │
│  - Prevent SQL injection                                 │
│  - Prevent XSS                                           │
└─────────────────────────────────────────────────────────┘

Result: Defense in Depth ✅
```

---

## Request Processing Flow

```
User Request: GET /api/session/info
Cookie: ARCHIVESESSION=abc123

┌─────────────────────────────────────────┐
│ 1. Request arrives at server            │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 2. SessionRepositoryFilter              │
│    - Extract cookie value: abc123       │
│    - Query database for session         │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 3. Database Query:                      │
│    SELECT * FROM SPRING_SESSION         │
│    WHERE SESSION_ID = 'abc123'          │
└────────────┬────────────────────────────┘
             │
             ├─── NOT FOUND ──►
             │                  │
             │                  ▼
             │         ┌────────────────┐
             │         │ Return 401     │
             │         │ Unauthorized   │
             │         └────────────────┘
             │
             └─── FOUND ──►
                          │
                          ▼
             ┌──────────────────────────────┐
             │ 4. Check EXPIRY_TIME         │
             │    Current > Expiry?         │
             └────────────┬─────────────────┘
                          │
                          ├─── EXPIRED ──►
                          │               │
                          │               ▼
                          │      ┌────────────────┐
                          │      │ Delete session │
                          │      │ Return 401     │
                          │      └────────────────┘
                          │
                          └─── VALID ──►
                                        │
                                        ▼
             ┌──────────────────────────────────┐
             │ 5. Load session attributes       │
             │    SELECT * FROM                 │
             │    SPRING_SESSION_ATTRIBUTES     │
             │    WHERE SESSION_PRIMARY_ID = X  │
             └────────────┬─────────────────────┘
                          │
                          ▼
             ┌──────────────────────────────────┐
             │ 6. Deserialize SPRING_SECURITY   │
             │    _CONTEXT attribute            │
             │    - Get Authentication object   │
             │    - Get User details            │
             └────────────┬─────────────────────┘
                          │
                          ▼
             ┌──────────────────────────────────┐
             │ 7. Set SecurityContext           │
             │    SecurityContextHolder         │
             │    .setContext(context)          │
             └────────────┬─────────────────────┘
                          │
                          ▼
             ┌──────────────────────────────────┐
             │ 8. Update LAST_ACCESS_TIME       │
             │    UPDATE SPRING_SESSION         │
             │    SET LAST_ACCESS_TIME = NOW()  │
             └────────────┬─────────────────────┘
                          │
                          ▼
             ┌──────────────────────────────────┐
             │ 9. Proceed to controller         │
             │    SessionController.info()      │
             └────────────┬─────────────────────┘
                          │
                          ▼
             ┌──────────────────────────────────┐
             │ 10. Build response with          │
             │     session information          │
             └────────────┬─────────────────────┘
                          │
                          ▼
             ┌──────────────────────────────────┐
             │ 11. Return JSON response         │
             │     + Set-Cookie (if modified)   │
             └──────────────────────────────────┘
```

---

## Configuration Flow

```
Application Startup Sequence:

1. Load application.properties
   └─► spring.session.store-type=jdbc
   └─► spring.session.timeout=30m
   └─► spring.flyway.enabled=true

2. Flyway Migration
   └─► Check migration status
   └─► Run V1__Create_Spring_Session_Tables.sql
   └─► Create SPRING_SESSION tables
   └─► Create indexes

3. HikariCP Initialization
   └─► Create connection pool
   └─► Max pool size: 10 (dev), 20 (prod)
   └─► Test database connectivity

4. SessionConfig Bean Creation
   └─► @EnableJdbcHttpSession
   └─► Create CookieSerializer bean
       └─► Set cookie name: ARCHIVESESSION
       └─► Set HttpOnly: true
       └─► Set Secure: based on profile
       └─► Set SameSite: based on profile

5. SecurityConfig Bean Creation
   └─► Configure session management
   └─► Set sessionFixation().newSession()
   └─► Set maximumSessions(5)

6. JdbcIndexedSessionRepository Creation
   └─► Wire DataSource
   └─► Initialize session repository
   └─► Start cleanup task (cron schedule)

7. Application Ready
   └─► Listen on port 8080
   └─► Ready to accept requests
```

---

These diagrams provide visual understanding of the complete session management system architecture, flows, and security layers.
