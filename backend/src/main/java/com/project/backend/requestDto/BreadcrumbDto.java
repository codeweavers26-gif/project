package com.project.backend.requestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreadcrumbDto {
	  private Long id;
	private String level;
	private String name;
	private String link;
	   private String slug;
	   private String type; 
	   private String url;
}