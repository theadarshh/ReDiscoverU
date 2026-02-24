package com.rediscoveru.service;

import com.rediscoveru.entity.ProgramCategory;
import com.rediscoveru.repository.ProgramCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service @RequiredArgsConstructor
public class CategoryService {

    private final ProgramCategoryRepository categoryRepo;

    public List<ProgramCategory> getAll()    { return categoryRepo.findAllByOrderByNameAsc(); }
    public List<ProgramCategory> getActive() { return categoryRepo.findByActiveTrueOrderByNameAsc(); }

    public ProgramCategory create(ProgramCategory cat) {
        if (cat.getName() == null || cat.getName().isBlank())
            throw new RuntimeException("Category name is required");
        cat.setName(cat.getName().trim());
        if (categoryRepo.findByNameIgnoreCase(cat.getName()).isPresent())
            throw new RuntimeException("Category already exists: " + cat.getName());
        return categoryRepo.save(cat);
    }

    public ProgramCategory update(Long id, ProgramCategory updated) {
        ProgramCategory existing = categoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        if (updated.getName() != null && !updated.getName().isBlank())
            existing.setName(updated.getName().trim());
        if (updated.getDescription() != null)
            existing.setDescription(updated.getDescription());
        existing.setActive(updated.isActive());
        return categoryRepo.save(existing);
    }

    public void toggle(Long id) {
        ProgramCategory c = categoryRepo.findById(id).orElseThrow();
        c.setActive(!c.isActive());
        categoryRepo.save(c);
    }

    public void delete(Long id) {
        categoryRepo.deleteById(id);
    }

    public void seedDefaults() {
        seed("Morning Rituals");
        seed("Growth Circles");
        seed("1-to-1 Mentorship");
    }

    private void seed(String name) {
        if (categoryRepo.findByNameIgnoreCase(name).isEmpty()) {
            ProgramCategory c = new ProgramCategory();
            c.setName(name);
            categoryRepo.save(c);
        }
    }
}
