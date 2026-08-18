package com.formquid.template.dto;

import com.formquid.content.dto.AddBlockItemRequestDto;
import com.formquid.content.dto.AddFixedQuestionItemRequestDto;
import com.formquid.content.dto.CreateQuestionItemRequestDto;
import com.formquid.content.dto.CreateTextItemRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateTemplateRequestDto(
    @NotBlank String title,
    String description,
    String imageKey,
    List<@Valid CreateTextItemRequestDto> texts,
    List<@Valid CreateQuestionItemRequestDto> localQuestions,
    List<@Valid AddFixedQuestionItemRequestDto> fixedQuestions,
    List<@Valid AddBlockItemRequestDto> blocks
) {
}
