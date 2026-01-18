# نظام التقارير - Report System Documentation

## 📋 نظرة عامة | Overview

نظام التقارير في مشروع University Archiving System يوفر إمكانية إنشاء وعرض وتصدير تقارير شاملة حول حالة التسليمات الأكاديمية عبر الأقسام والفصول الدراسية.

The Report System provides comprehensive reporting capabilities for academic submission tracking across departments and semesters.

---

## 🏗️ هيكل النظام | System Architecture

### Backend Components

#### 1. DTOs (Data Transfer Objects)
| الملف | الوصف |
|-------|-------|
| [ProfessorSubmissionReport.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/ProfessorSubmissionReport.java) | تقرير تسليمات الأساتذة لفصل دراسي |
| [SystemWideReport.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/SystemWideReport.java) | تقرير شامل على مستوى النظام |
| [DepartmentSubmissionReport.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/DepartmentSubmissionReport.java) | تقرير تسليمات القسم |
| [DepartmentReportSummary.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/DepartmentReportSummary.java) | ملخص إحصائيات القسم |
| [SubmissionStatistics.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/SubmissionStatistics.java) | إحصائيات التسليمات |
| [ProfessorSubmissionRow.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/ProfessorSubmissionRow.java) | صف بيانات الأستاذ في التقرير |
| [DocumentStatusInfo.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/DocumentStatusInfo.java) | معلومات حالة المستند |
| [ReportFilter.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/ReportFilter.java) | فلاتر التقارير |
| [ReportFilterOptions.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/ReportFilterOptions.java) | خيارات الفلترة المتاحة |
| [DashboardOverview.java](../src/main/java/com/alquds/edu/ArchiveSystem/dto/report/DashboardOverview.java) | نظرة عامة على لوحة التحكم |

#### 2. Services
| الخدمة | الوصف |
|--------|-------|
| [SemesterReportService.java](../src/main/java/com/alquds/edu/ArchiveSystem/service/academic/SemesterReportService.java) | واجهة خدمة التقارير الرئيسية |
| [SemesterReportServiceImpl.java](../src/main/java/com/alquds/edu/ArchiveSystem/service/academic/SemesterReportServiceImpl.java) | تنفيذ خدمة التقارير |
| [PdfReportService.java](../src/main/java/com/alquds/edu/ArchiveSystem/service/report/PdfReportService.java) | خدمة تصدير PDF باستخدام iTextPDF |

#### 3. Controllers (API Endpoints)
| المتحكم | الـ Endpoints |
|---------|---------------|
| [HodController.java](../src/main/java/com/alquds/edu/ArchiveSystem/controller/api/HodController.java) | `/api/hod/reports/*` |
| [DeanshipController.java](../src/main/java/com/alquds/edu/ArchiveSystem/controller/api/DeanshipController.java) | `/api/deanship/reports/*` |
| [AdminController.java](../src/main/java/com/alquds/edu/ArchiveSystem/controller/api/AdminController.java) | `/api/admin/reports/*` |

### Frontend Components

#### JavaScript Modules
| الملف | الوصف |
|-------|-------|
| [deanship-reports.js](../src/main/resources/static/js/deanship/deanship-reports.js) | لوحة تقارير عمادة الكلية |
| [deanship-export.js](../src/main/resources/static/js/deanship/deanship-export.js) | خدمة التصدير للعمادة |
| [hod-reports.js](../src/main/resources/static/js/shared/hod-reports.js) | تقارير رئيس القسم |

---

## 📊 أنواع التقارير | Report Types

### 1. Professor Submission Report (تقرير تسليمات الأساتذة)
```java
public class ProfessorSubmissionReport {
    Long semesterId;
    String semesterName;
    Long departmentId;
    String departmentName;
    LocalDateTime generatedAt;
    String generatedBy;
    List<ProfessorSubmissionRow> rows;
    SubmissionStatistics statistics;
}
```

**الاستخدام:** يستخدمه رئيس القسم (HOD) لمراقبة حالة التسليمات للأساتذة في قسمه.

### 2. System-Wide Report (التقرير الشامل)
```java
public class SystemWideReport {
    Long semesterId;
    String semesterName;
    LocalDateTime generatedAt;
    String generatedBy;
    List<DepartmentReportSummary> departmentSummaries;
    SubmissionStatistics overallStatistics;
}
```

**الاستخدام:** يستخدمه عمادة الكلية لعرض إحصائيات شاملة عبر جميع الأقسام.

### 3. Department Submission Report (تقرير تسليمات القسم)
```java
public class DepartmentSubmissionReport {
    String departmentName;
    LocalDateTime generatedAt;
    String generatedBy;
    int totalProfessors;
    int totalRequests;
    int totalSubmitted;
    int totalPending;
    int totalOverdue;
    Double overallCompletionRate;
    Double overallOnTimeRate;
    List<ProfessorSubmissionSummary> professorSummaries;
}
```

---

## 🔌 API Endpoints

### HOD Endpoints (رئيس القسم)

| Method | Endpoint | الوصف |
|--------|----------|-------|
| `GET` | `/api/hod/reports/professor-submissions` | الحصول على تقرير تسليمات الأساتذة |
| `GET` | `/api/hod/reports/professor-submissions/pdf` | تصدير التقرير كـ PDF |
| `GET` | `/api/hod/submissions/status` | حالة التسليمات مع الفلترة |

**Parameters:**
- `semesterId` (required): معرف الفصل الدراسي
- `courseCode` (optional): رمز المساق
- `documentType` (optional): نوع المستند
- `status` (optional): حالة التسليم

### Deanship Endpoints (العمادة)

| Method | Endpoint | الوصف |
|--------|----------|-------|
| `GET` | `/api/deanship/reports/system-wide` | تقرير شامل على مستوى النظام |
| `GET` | `/api/deanship/reports/filter-options` | خيارات الفلترة المتاحة |

**Parameters:**
- `semesterId` (required): معرف الفصل الدراسي

### Admin Endpoints (المدير)

| Method | Endpoint | الوصف |
|--------|----------|-------|
| `GET` | `/api/admin/reports/filter-options` | خيارات الفلترة |
| `GET` | `/api/admin/reports/overview` | نظرة عامة شاملة |

---

## 📈 الإحصائيات المتوفرة | Available Statistics

### SubmissionStatistics
```java
public class SubmissionStatistics {
    Integer totalProfessors;      // إجمالي الأساتذة
    Integer totalCourses;          // إجمالي المساقات
    Integer totalRequiredDocuments; // إجمالي المستندات المطلوبة
    Integer submittedDocuments;    // المستندات المسلمة
    Integer missingDocuments;      // المستندات الناقصة
    Integer overdueDocuments;      // المستندات المتأخرة
}
```

### حالات التسليم | Submission Statuses
| الحالة | الوصف | اللون |
|--------|-------|-------|
| `UPLOADED` | تم التسليم | 🟢 أخضر |
| `NOT_UPLOADED` | لم يتم التسليم | ⚪ رمادي |
| `OVERDUE` | متأخر | 🔴 أحمر |

---

## 📄 تصدير PDF | PDF Export

### الميزات
- **Header:** شعار جامعة القدس + عنوان التقرير
- **Metadata:** اسم المُنشئ + تاريخ الإنشاء
- **Statistics Table:** جدول الإحصائيات الملونة
- **Data Table:** جدول تفصيلي للأساتذة والمساقات
- **Status Indicators:** مؤشرات الحالة الملونة
- **Legend:** شرح الرموز
- **Footer:** تذييل الصفحة

### التقنيات المستخدمة
- **Backend:** iTextPDF 7.x
- **Frontend:** jsPDF + autoTable

### مثال على الكود:
```java
// Backend PDF Generation
public byte[] generateProfessorSubmissionReportPdf(ProfessorSubmissionReport report) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        
        addSemesterReportHeader(document, report);
        addSemesterStatistics(document, report);
        addProfessorSubmissionTable(document, report);
        addFooter(document);
        
        document.close();
        return baos.toByteArray();
    }
}
```

---

## 🎨 Frontend Report Dashboard

### ReportsDashboard Class
```javascript
class ReportsDashboard {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.currentView = 'department';
        this.currentData = null;
        this.filters = {};
        this.isLoading = false;
    }
    
    // View Types: 'department' | 'level' | 'semester'
    async setView(viewType) { ... }
    async loadData() { ... }
    async exportReport(format) { ... }
}
```

### ExportService Class (Frontend)
```javascript
class ExportService {
    static async exportToPDF(data, options) { ... }
    static async exportToExcel(data, options) { ... }
    static async exportTableToPDF(data, columns, title) { ... }
}
```

---

## 🔒 Role-Based Access Control

| الدور | الصلاحيات |
|-------|-----------|
| **HOD** | تقارير القسم الخاص فقط |
| **DEANSHIP** | تقارير جميع الأقسام |
| **ADMIN** | جميع التقارير + خيارات فلترة كاملة |

### Department Scoping
```java
// For HOD users, automatically applies department filter
ProfessorSubmissionReport generateProfessorSubmissionReportWithRoleFilter(
    Long semesterId, 
    Long departmentId, 
    User currentUser
);
```

---

## 🔧 Filter Options

### ReportFilterOptions
```java
public class ReportFilterOptions {
    List<DepartmentOption> departments;
    List<CourseOption> courses;
    List<ProfessorOption> professors;
    List<AcademicYearOption> academicYears;
    List<SemesterOption> semesters;
    List<DocumentTypeEnum> documentTypes;
    List<SubmissionStatus> submissionStatuses;
    boolean canFilterByDepartment;
    Long userDepartmentId;
    String userDepartmentName;
}
```

### ReportFilter
```java
public class ReportFilter {
    String courseCode;
    DocumentTypeEnum documentType;
    SubmissionStatus status;
}
```

---

## 📱 UI Components

### HOD Dashboard - Reports Section
- **Generate Report Button:** إنشاء التقرير
- **Export PDF Button:** تصدير PDF
- **Clear Filters Button:** مسح الفلاتر
- **Pagination Controls:** التحكم بالصفحات
- **Statistics Summary:** ملخص الإحصائيات
- **Data Table:** جدول البيانات

### Deanship Dashboard - Reports Section
- **View Toggle:** التبديل بين العروض (By Department / By Course Level / By Semester)
- **Export PDF Button:** تصدير PDF
- **Export Excel Button:** تصدير Excel
- **Interactive Charts:** رسوم بيانية تفاعلية

---

## 🧪 Testing

### Integration Tests
```
src/test/java/com/alquds/edu/ArchiveSystem/controller/api/
├── HodControllerIntegrationTest.java
├── DeanshipControllerIntegrationTest.java
└── AdminControllerIntegrationTest.java
```

### E2E Tests
```
testsprite_tests/
├── TC006_test_report_generation_and_pdf_export.py
└── ...
```

---

## 📊 Database Queries (Optimized)

```java
// Optimized query with eager loading to prevent N+1
List<CourseAssignment> courseAssignments = 
    courseAssignmentRepository.findBySemesterIdWithEagerLoading(semesterId);
```

---

## 🚀 الميزات المستقبلية | Future Enhancements

1. **📧 Email Reports:** إرسال التقارير عبر البريد الإلكتروني
2. **📅 Scheduled Reports:** جدولة التقارير التلقائية
3. **📊 Advanced Charts:** رسوم بيانية متقدمة
4. **🔔 Alert System:** نظام تنبيهات للتأخيرات
5. **📈 Trend Analysis:** تحليل الاتجاهات عبر الفصول

---

## 📝 ملاحظات المطور | Developer Notes

### Performance Considerations
- استخدام Eager Loading لتجنب مشكلة N+1 queries
- Pagination للبيانات الكبيرة
- Caching للبيانات الثابتة

### Security
- JWT Authentication لجميع الـ endpoints
- Role-based authorization
- Department scoping للـ HOD

---

*آخر تحديث: January 2026*
