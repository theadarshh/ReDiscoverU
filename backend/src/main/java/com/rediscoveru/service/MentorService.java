package com.rediscoveru.service;

import com.rediscoveru.entity.Mentor;
import com.rediscoveru.repository.MentorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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
        Mentor m = mentorRepo.findById(id).orElseThrow(() -> new RuntimeException("Mentor not found"));
        if (updates.getName() != null)        m.setName(updates.getName());
        if (updates.getRoleTitle() != null)   m.setRoleTitle(updates.getRoleTitle());
        if (updates.getBio() != null)         m.setBio(updates.getBio());
        if (updates.getEmail() != null)       m.setEmail(updates.getEmail());
        if (updates.getWhatsappLink() != null) m.setWhatsappLink(updates.getWhatsappLink());
        if (updates.getImageUrl() != null)    m.setImageUrl(updates.getImageUrl());
        m.setActive(updates.isActive());
        m.setDisplayOrder(updates.getDisplayOrder());
        return mentorRepo.save(m);
    }

    public Mentor toggle(Long id) {
        Mentor m = mentorRepo.findById(id).orElseThrow(() -> new RuntimeException("Mentor not found"));
        m.setActive(!m.isActive());
        return mentorRepo.save(m);
    }

    public void delete(Long id) {
        Mentor m = mentorRepo.findById(id).orElseThrow(() -> new RuntimeException("Mentor not found"));
        m.setDeleted(true);
        mentorRepo.save(m);
    }

    public Mentor uploadImage(Long id, MultipartFile file) throws IOException {
        Mentor m = mentorRepo.findById(id).orElseThrow(() -> new RuntimeException("Mentor not found"));
        java.nio.file.Path dir = Paths.get(uploadDir, "mentors");
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" +
            (file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "photo.jpg");
        Files.copy(file.getInputStream(), dir.resolve(filename));
        m.setImageUrl("/uploads/mentors/" + filename);
        return mentorRepo.save(m);
    }

    public void seedDefault() {
        if (mentorRepo.count() == 0) {
            Mentor m = new Mentor();
            m.setName("Jayashankar Lingaiah");
            m.setRoleTitle("Lifestyle & Mindset Coach");
            m.setBio("Jayashankar is a Lifestyle and Mindset Coach dedicated to helping students and working professionals build structured, meaningful lives. His approach blends practical habit design, emotional intelligence, and personal accountability — free from hype, free from false promises.");
            m.setEmail("jayashankarkol@gmail.com");
            m.setWhatsappLink("https://wa.me/919900532001");
            m.setActive(true);
            m.setDisplayOrder(0);
            mentorRepo.save(m);
        }
    }
}
