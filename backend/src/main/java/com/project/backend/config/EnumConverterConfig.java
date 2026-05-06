package com.project.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentStatus;

@Configuration
public class EnumConverterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, OrderStatus.class,
                s -> OrderStatus.valueOf(s.toUpperCase()));
        registry.addConverter(String.class, PaymentStatus.class,
                s -> PaymentStatus.valueOf(s.toUpperCase()));
    }
}
