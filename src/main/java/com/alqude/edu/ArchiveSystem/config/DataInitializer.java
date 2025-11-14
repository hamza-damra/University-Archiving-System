package com.alqude.edu.ArchiveSystem.config;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final SubmittedDocumentRepository submittedDocumentRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        initializeData();
    }
    
    private void initializeData() {
        log.info("Initializing application data...");
        
        // Create departments if they don't exist
        Department csDepartment = createDepartmentIfNotExists("Computer Science", "Department of Computer Science");
        Department mathDepartment = createDepartmentIfNotExists("Mathematics", "Department of Mathematics");
        Department physicsDepartment = createDepartmentIfNotExists("Physics", "Department of Physics");
        
        // Create default HOD users
        User hodCs = createUserIfNotExists(
                "hod.cs@alquds.edu", 
                "password123", 
                "Ahmad", 
                "Al-Rashid", 
                Role.ROLE_HOD, 
                csDepartment
        );
        
        User hodMath = createUserIfNotExists(
                "hod.math@alquds.edu", 
                "password123", 
                "Fatima", 
                "Al-Zahra", 
                Role.ROLE_HOD, 
                mathDepartment
        );
        
        // Use hodMath for creating math department requests if needed later
        
        // Create default professor users
        User profOmar = createUserIfNotExists(
                "prof.omar@alquds.edu", 
                "password123", 
                "Omar", 
                "Al-Khouri", 
                Role.ROLE_PROFESSOR, 
                csDepartment
        );
        
        User profLayla = createUserIfNotExists(
                "prof.layla@alquds.edu", 
                "password123", 
                "Layla", 
                "Al-Mansouri", 
                Role.ROLE_PROFESSOR, 
                csDepartment
        );
        
        User profHassan = createUserIfNotExists(
                "prof.hassan@alquds.edu", 
                "password123", 
                "Hassan", 
                "Al-Tamimi", 
                Role.ROLE_PROFESSOR, 
                mathDepartment
        );
        
        User profNour = createUserIfNotExists(
                "prof.nour@alquds.edu", 
                "password123", 
                "Nour", 
                "Al-Qasemi", 
                Role.ROLE_PROFESSOR, 
                physicsDepartment
        );
        
        // Create sample document requests
        createSampleDocumentRequests(hodCs, profOmar, profLayla, profHassan, profNour);
        
        // Create sample notifications
        createSampleNotifications(profOmar, profLayla, profHassan);
        
        log.info("Data initialization completed successfully");
    }
    
    private Department createDepartmentIfNotExists(String name, String description) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setName(name);
                    department.setDescription(description);
                    Department saved = departmentRepository.save(department);
                    log.info("Created department: {}", name);
                    return saved;
                });
    }
    
    private User createUserIfNotExists(String email, String password, String firstName, 
                                     String lastName, Role role, Department department) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setRole(role);
                    user.setDepartment(department);
                    user.setIsActive(true);
                    User saved = userRepository.save(user);
                    log.info("Created user: {} ({}) in department: {}", 
                            email, role, department.getName());
                    return saved;
                });
    }
    
    private void createSampleDocumentRequests(User hodCs, User profOmar, User profLayla, User profHassan, User profNour) {
        log.info("Creating sample document requests...");
        
        // Computer Science requests
        createDocumentRequest(
            "Advanced Algorithms", 
            "Research Paper", 
            Arrays.asList("pdf", "doc", "docx"), 
            LocalDateTime.now().plusDays(7),
            profOmar,
            hodCs,
            "Submit your final research paper on advanced algorithms",
            3,
            25
        );
        
        createDocumentRequest(
            "Database Systems", 
            "Project Documentation", 
            Arrays.asList("pdf", "zip"), 
            LocalDateTime.now().plusDays(14),
            profLayla,
            hodCs,
            "Complete project documentation with ER diagrams and schemas",
            5,
            50
        );
        
        // Mathematics requests  
        createDocumentRequest(
            "Linear Algebra", 
            "Assignment Solutions", 
            Arrays.asList("pdf", "docx"), 
            LocalDateTime.now().plusDays(5),
            profHassan,
            hodCs,
            "Submit solutions for chapters 1-5",
            2,
            15
        );
        
        // Physics requests
        createDocumentRequest(
            "Quantum Mechanics", 
            "Lab Report", 
            Arrays.asList("pdf", "xlsx", "csv"), 
            LocalDateTime.now().plusDays(10),
            profNour,
            hodCs,
            "Lab report with experimental data and analysis",
            4,
            30
        );
        
        // Create some submitted documents
        createSampleSubmittedDocuments();
    }
    
    private void createDocumentRequest(String courseName, String documentType, List<String> extensions, 
                                     LocalDateTime deadline, User professor, User createdBy, 
                                     String description, Integer maxFileCount, Integer maxSizeMb) {
        DocumentRequest request = new DocumentRequest();
        request.setCourseName(courseName);
        request.setDocumentType(documentType);
        request.setRequiredFileExtensions(extensions);
        request.setDeadline(deadline);
        request.setProfessor(professor);
        request.setCreatedBy(createdBy);
        request.setDescription(description);
        request.setMaxFileCount(maxFileCount);
        request.setMaxTotalSizeMb(maxSizeMb);
        
        documentRequestRepository.save(request);
        log.info("Created document request: {} for professor: {}", courseName, professor.getEmail());
    }
    
    private void createSampleSubmittedDocuments() {
        log.info("Creating sample submitted documents...");
        
        List<DocumentRequest> requests = documentRequestRepository.findAll();
        if (requests.size() >= 2) {
            // Get all request IDs and check existing submissions in bulk
            List<Long> requestIds = requests.stream()
                    .map(DocumentRequest::getId)
                    .toList();
            
            List<SubmittedDocument> existingSubmissions = submittedDocumentRepository.findByDocumentRequestIds(requestIds);
            List<Long> existingRequestIds = existingSubmissions.stream()
                    .map(sd -> sd.getDocumentRequest().getId())
                    .toList();
            
            // Create a submitted document for the first request that doesn't already have one
            DocumentRequest request1 = requests.stream()
                    .filter(req -> !existingRequestIds.contains(req.getId()))
                    .findFirst()
                    .orElse(null);
            
            if (request1 != null) {
                SubmittedDocument submittedDoc1 = new SubmittedDocument();
                submittedDoc1.setDocumentRequest(request1);
                submittedDoc1.setProfessor(request1.getProfessor());
                submittedDoc1.setOriginalFilename("algorithms_research.pdf");
                submittedDoc1.setFileUrl("submitted/algorithms_research_" + System.currentTimeMillis() + ".pdf");
                submittedDoc1.setFileSize(2048576L); // 2MB
                submittedDoc1.setFileType("application/pdf");
                submittedDoc1.setFileCount(1);
                submittedDoc1.setTotalFileSize(2048576L);
                submittedDoc1.setNotes("Final research paper submission");
                submittedDoc1.setSubmittedAt(LocalDateTime.now().minusDays(2));
                submittedDoc1.setIsLateSubmission(false);
                
                submittedDocumentRepository.save(submittedDoc1);
                log.info("Created submitted document for request: {}", request1.getCourseName());
            }
            
            // Create a late submission for the second request that doesn't already have one
            DocumentRequest request2 = requests.stream()
                    .filter(req -> !existingRequestIds.contains(req.getId()))
                    .skip(1)
                    .findFirst()
                    .orElse(null);
            
            if (request2 != null) {
                SubmittedDocument submittedDoc2 = new SubmittedDocument();
                submittedDoc2.setDocumentRequest(request2);
                submittedDoc2.setProfessor(request2.getProfessor());
                submittedDoc2.setOriginalFilename("database_project.zip");
                submittedDoc2.setFileUrl("submitted/database_project_" + System.currentTimeMillis() + ".zip");
                submittedDoc2.setFileSize(5242880L); // 5MB
                submittedDoc2.setFileType("application/zip");
                submittedDoc2.setFileCount(3);
                submittedDoc2.setTotalFileSize(8388608L); // 8MB total
                submittedDoc2.setNotes("Project files with documentation");
                submittedDoc2.setSubmittedAt(LocalDateTime.now().plusDays(1)); // Late submission
                submittedDoc2.setIsLateSubmission(true);
                
                submittedDocumentRepository.save(submittedDoc2);
                log.info("Created late submitted document for request: {}", request2.getCourseName());
            }
        }
    }
    
    private void createSampleNotifications(User profOmar, User profLayla, User profHassan) {
        log.info("Creating sample notifications...");
        
        // Notification for Omar
        createNotification(
            profOmar,
            "New Document Request Assigned",
            "You have been assigned a new document request: Advanced Algorithms",
            Notification.NotificationType.NEW_REQUEST
        );
        
        createNotification(
            profOmar,
            "Deadline Approaching",
            "Database Systems project deadline is in 3 days",
            Notification.NotificationType.DEADLINE_APPROACHING
        );
        
        // Notification for Layla
        createNotification(
            profLayla,
            "Submission Received",
            "A new submission has been received for Database Systems",
            Notification.NotificationType.DOCUMENT_SUBMITTED
        );
        
        // Notification for Hassan
        createNotification(
            profHassan,
            "Document Request Created",
            "Linear Algebra assignment request has been created",
            Notification.NotificationType.NEW_REQUEST
        );
    }
    
    private void createNotification(User user, String title, String message, Notification.NotificationType type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now().minusHours((long)(Math.random() * 24)));
        
        notificationRepository.save(notification);
        log.info("Created notification for user: {} - {}", user.getEmail(), title);
    }
}
