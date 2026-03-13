package com.project.backend.ResponseDto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnTimelineDto {
	  private Long id;
	    private Long returnId;
	    private String status; 
	    private String title;
	    private String description;
	    private LocalDateTime createdAt;
	    private String createdBy;
	    private String createdByRole;
	    private String metadata;
}