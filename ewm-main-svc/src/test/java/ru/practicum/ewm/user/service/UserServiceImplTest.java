package ru.practicum.ewm.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.exception.DuplicatedDataException;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_whenEmailUnique_thenReturnsSavedUser() {
        NewUserRequest input = NewUserRequest.builder()
                .name("Иван Петров")
                .email("ivan@mail.ru")
                .build();
        User saved = User.builder().id(1L).name("Иван Петров").email("ivan@mail.ru").build();
        when(userRepository.existsByEmail("ivan@mail.ru")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserDto result = userService.create(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("ivan@mail.ru");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_whenEmailExists_thenThrowsAndDoesNotSave() {
        NewUserRequest input = NewUserRequest.builder()
                .name("Дубликат")
                .email("dup@mail.ru")
                .build();
        when(userRepository.existsByEmail("dup@mail.ru")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(input))
                .isInstanceOf(DuplicatedDataException.class)
                .hasMessageContaining("dup@mail.ru");

        verify(userRepository, never()).save(any());
    }
}
