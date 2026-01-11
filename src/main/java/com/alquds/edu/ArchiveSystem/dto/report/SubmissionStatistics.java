package com.alquds.edu.ArchiveSystem.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionStatistics {
    private Integer totalProfessors;
    private Integer totalCourses;
    private Integer totalRequiredDocuments;
    private Integer submittedDocuments;
    private Integer missingDocuments;
    private Integer overdueDocuments;
}
