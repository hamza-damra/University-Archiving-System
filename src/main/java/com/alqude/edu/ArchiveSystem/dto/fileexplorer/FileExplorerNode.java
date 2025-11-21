package com.alqude.edu.ArchiveSystem.dto.fileexplorer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileExplorerNode {
    
    private String path;
    private String name;
    private NodeType type;
    private Long entityId;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @Builder.Default
    private List<FileExplorerNode> children = new ArrayList<>();
    
    @Builder.Default
    private List<UploadedFileDTO> files = new ArrayList<>();
    
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
}
