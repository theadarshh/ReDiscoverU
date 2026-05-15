package com.rediscoveru.service;

import com.rediscoveru.entity.Mentor;
import com.rediscoveru.repository.MentorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * MentorService
 *
 * Image upload strategy:
 *   Always saved as  uploads/mentors/mentor-{id}.jpg
 *   Fixed filename = automatic overwrite = no accumulation of old files.
 *   Served at: http://localhost:8080/uploads/mentors/mentor-{id}.jpg
 */
@Service @RequiredArgsConstructor
public class MentorService {

    private final MentorRepository mentorRepo;

    @Value("${file.upload.dir:uploads}") private String uploadDir;

    public List<Mentor> getAll() {
        return mentorRepo.findByDeletedFalseOrderByDisplayOrderAscCreatedAtAsc();
    }

    public List<Mentor> getActive() {
        return mentorRepo.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscCreatedAtAsc();
    }

    public Mentor create(Mentor m) {
        m.setDeleted(false);
        return mentorRepo.save(m);
    }

    public Mentor update(Long id, Mentor updates) {
        Mentor m = mentorRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        if (updates.getName()         != null) m.setName(updates.getName());
        if (updates.getRoleTitle()    != null) m.setRoleTitle(updates.getRoleTitle());
        if (updates.getBio()          != null) m.setBio(updates.getBio());
        if (updates.getEmail()        != null) m.setEmail(updates.getEmail());
        if (updates.getWhatsappLink() != null) m.setWhatsappLink(updates.getWhatsappLink());
        if (updates.getImageUrl()     != null) m.setImageUrl(updates.getImageUrl());
        m.setActive(updates.isActive());
        m.setDisplayOrder(updates.getDisplayOrder());
        return mentorRepo.save(m);
    }

    public Mentor toggle(Long id) {
        Mentor m = mentorRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        m.setActive(!m.isActive());
        return mentorRepo.save(m);
    }

    public void delete(Long id) {
        Mentor m = mentorRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        m.setDeleted(true);
        mentorRepo.save(m);
    }

    /**
     * Upload mentor photo.
     * Fixed filename → always overwrites → only one file on disk per mentor.
     * URL returned: /uploads/mentors/mentor-{id}.jpg
     */
    public Mentor uploadImage(Long id, MultipartFile file) throws IOException {
        Mentor m = mentorRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));

        // Derive extension from original filename (jpg/png/webp etc.)
        String ext = "jpg";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }

        // Fixed filename per mentor — always overwrites old image
        String filename = "mentor-" + id + "." + ext;

        Path dir  = Paths.get(uploadDir, "mentors");
        Files.createDirectories(dir);

        Path dest = dir.resolve(filename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = "/uploads/mentors/" + filename;
        m.setImageUrl(imageUrl);
        return mentorRepo.save(m);
    }

    public void seedDefault() {
        if (mentorRepo.count() == 0) {
            Mentor m = new Mentor();
            m.setName("Jayashankar Lingaiah");
            m.setRoleTitle("Lifestyle & Mindset Coach");
            m.setBio("Jayashankar is a Lifestyle and Mindset Coach dedicated to helping students " +
                     "and working professionals build structured, meaningful lives. His approach " +
                     "blends practical habit design, emotional intelligence, and personal accountability" +
                     " — free from hype, free from false promises.");
            m.setEmail("jayashankarkol@gmail.com");
            m.setWhatsappLink("https://wa.me/919900532001");
            m.setActive(true);
            m.setDisplayOrder(0);
            mentorRepo.save(m);
        }
    }
}
