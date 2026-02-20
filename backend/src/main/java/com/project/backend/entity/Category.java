package com.project.backend.entity;
	
	import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
	@Entity
	@Table(name = "categories")
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public class Category {
	
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	
	    @ManyToOne
	    @JoinColumn(name = "section_id", nullable = false)
	    private Section section;
	
	    @Column(nullable = false)
	    private String name; // Clothing, Footwear
	
	    private Boolean isActive = true;
	    
	    @OneToMany(mappedBy = "category")
	    private List<SubCategory> subCategories = new ArrayList<>();
	}
