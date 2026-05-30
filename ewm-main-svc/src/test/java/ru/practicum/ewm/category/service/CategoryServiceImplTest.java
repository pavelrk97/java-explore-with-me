package ru.practicum.ewm.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.exception.DuplicatedDataException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void create_whenNameUnique_thenReturnsSavedCategory() {
        NewCategoryDto input = new NewCategoryDto();
        input.setName("Концерты");

        Category saved = Category.builder().id(1L).name("Концерты").build();
        when(categoryRepository.existsByName("Концерты")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryDto result = categoryService.create(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Концерты");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_whenNameExists_thenThrowsAndDoesNotSave() {
        // given
        NewCategoryDto input = new NewCategoryDto();
        input.setName("Дубликат");
        when(categoryRepository.existsByName("Дубликат")).thenReturn(true);

        // when + then
        assertThatThrownBy(() -> categoryService.create(input))
                .isInstanceOf(DuplicatedDataException.class)
                .hasMessageContaining("Дубликат");

        verify(categoryRepository, never()).save(any());
    }
}
