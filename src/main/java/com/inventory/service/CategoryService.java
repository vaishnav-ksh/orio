package com.inventory.service;

import com.inventory.dto.CategoryDto;
import com.inventory.entity.Category;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.InvalidOperationException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        return mapToDto(getCategoryEntity(id));
    }

    public CategoryDto createCategory(CategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new DuplicateResourceException("Category already exists with name: " + dto.getName());
        }
        Category category = new Category(dto.getName().trim(), dto.getDescription());
        Category saved = categoryRepository.save(category);
        return mapToDto(saved);
    }

    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        Category category = getCategoryEntity(id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(dto.getName().trim(), id)) {
            throw new DuplicateResourceException("Another category already exists with name: " + dto.getName());
        }
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        Category updated = categoryRepository.save(category);
        return mapToDto(updated);
    }

    public void deleteCategory(Long id) {
        Category category = getCategoryEntity(id);
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new InvalidOperationException("Cannot delete category with ID " + id + " because it contains " 
                    + category.getProducts().size() + " products.");
        }
        categoryRepository.delete(category);
    }

    private CategoryDto mapToDto(Category category) {
        long count = category.getProducts() != null ? category.getProducts().size() : 0;
        return new CategoryDto(category.getId(), category.getName(), category.getDescription(), count);
    }
}
