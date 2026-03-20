package com.rediscoveru.service;

import com.rediscoveru.entity.Program;
import com.rediscoveru.entity.UploadedFile;
import com.rediscoveru.repository.ProgramRepository;
import com.rediscoveru.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final UploadedFileRepository fileRepo;
    private final ProgramRepository      programRepo;
    private final CloudStorageService    cloudStorageService;

    @Value("${file.upload.dir:uploads}") private String uploadDir;

    public List<UploadedFile> getFiles(Long programId) {
        return fileRepo.findByProgramIdOrderByOrderIndexAscUploadedAtAsc(programId);
    }

    public UploadedFile upload(Long programId, MultipartFile file,
                               String title, int orderIndex) throws Exception {
        Program program = programRepo.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found: " + programId));

        String mime         = file.getContentType() != null ? file.getContentType() : "";
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";

        UploadedFile.FileCategory category = UploadedFile.FileCategory.fromMime(mime);
        if (category == UploadedFile.FileCategory.OTHER)
            category = UploadedFile.FileCategory.fromExtension(originalName);

        String urlPath;
        String diskPath = null;

        if (cloudStorageService.isEnabled()) {
            urlPath = cloudStorageService.upload(file, "programs/" + programId);
        } else {
            String safeName = UUID.randomUUID() + "_" +
                    originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path dir = Paths.get(uploadDir, "programs", programId.toString());
            Files.createDirectories(dir);
            Path dest = dir.resolve(safeName);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            urlPath  = "/uploads/programs/" + programId + "/" + safeName;
            diskPath = dest.toAbsolutePath().toString();
        }

        UploadedFile uf = new UploadedFile();
        uf.setProgram(program);
        uf.setTitle(title != null && !title.isBlank() ? title : originalName);
        uf.setFileName(originalName);
        uf.setFileCategory(category);
        uf.setContentType(mime);
        uf.setFilePath(urlPath);
        uf.setDiskPath(diskPath);
        uf.setFileSizeBytes(file.getSize());
        uf.setOrderIndex(orderIndex);
        return fileRepo.save(uf);
    }

    public void delete(Long fileId) throws Exception {
        UploadedFile uf = fileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        String filePath = uf.getFilePath();
        if (filePath != null && (filePath.startsWith("http://") || filePath.startsWith("https://"))) {
            cloudStorageService.delete(filePath);
        } else if (uf.getDiskPath() != null) {
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(uf.getDiskPath())); }
            catch (Exception ignored) {}
        }
        fileRepo.delete(uf);
    }

    public UploadedFile updateOrder(Long fileId, int newOrder) {
        UploadedFile uf = fileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        uf.setOrderIndex(newOrder);
        return fileRepo.save(uf);
    }
}
