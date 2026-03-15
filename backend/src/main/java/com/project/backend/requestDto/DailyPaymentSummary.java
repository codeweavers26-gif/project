package com.project.backend.requestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPaymentSummary {
    private String date;
    private Long count;
    private BigDecimal amount;
    private Long successCount;
    private Long failureCount;
}
