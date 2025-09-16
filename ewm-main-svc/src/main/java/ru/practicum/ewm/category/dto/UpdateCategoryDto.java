package ru.practicum.ewm.category.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryDto {
    @Size(min = 1, max = 50, message = "Длина name должна составлять от 1 до 50 символов!")
    String name;
}
