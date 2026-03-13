package com.project.backend.ResponseDto;

import java.util.List;

import com.project.backend.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDetailDto {
    private ReturnDto returnInfo;
    private OrderItemDto orderItem;
      private RefundDto refund;
    private List<ReturnTimelineDto> timeline;

} 
