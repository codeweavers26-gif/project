package com.project.backend.requestDto;

import lombok.Data;

@Data
public class ReturnItemRequestDto {
  private Long orderItemId;
    private Integer quantity;
}
