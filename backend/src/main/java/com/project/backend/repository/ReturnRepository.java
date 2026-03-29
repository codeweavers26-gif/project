package com.project.backend.repository;

import com.project.backend.ResponseDto.ProductReturnStatsDto;
import com.project.backend.entity.Return;
import com.project.backend.entity.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {

    Optional<Return> findByOrderId(Long orderId);

    Optional<Return> findByReturnNumber(String returnNumber);

    @Query("SELECT r FROM Return r JOIN r.items ri WHERE ri.orderItemId = :orderItemId")
    Optional<Return> findByOrderItemId(@Param("orderItemId") Long orderItemId);
    
    @Query("SELECT COUNT(r) > 0 FROM Return r JOIN r.items ri WHERE ri.orderItemId = :orderItemId")
boolean existsByOrderItemId(@Param("orderItemId") Long orderItemId);

    List<Return> findByUserId(Long userId);

    Page<Return> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(r) FROM Return r WHERE r.status IN :statuses")
    Long countByStatusIn(@Param("statuses") List<String> statuses);
    
    @Query("SELECT r.status as status, COUNT(r) as count FROM Return r GROUP BY r.status")
    Map<String, Long> countByStatusGrouped();
    
    @Query("SELECT r FROM Return r WHERE r.status = :status")
    List<Return> findByStatus(@Param("status") String status);
    
    @Query("SELECT r FROM Return r WHERE r.status IN :statuses")
    List<Return> findByStatusIn(@Param("statuses") List<String> statuses);

    Long countByStatus(String status);

@Query("SELECT COUNT(r) FROM Return r WHERE r.user.id = :userId AND r.status IN :statuses")
Long countByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<String> statuses);

@Query("""
        SELECT DISTINCT r
        FROM Return r
        LEFT JOIN r.items ri
        LEFT JOIN OrderItem oi ON oi.id = ri.orderItemId
        WHERE 
        (:status IS NULL OR r.status = :status)
        AND (
            :searchTerm IS NULL 
            OR LOWER(r.returnNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(oi.productName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        AND (:productId IS NULL OR oi.productId = :productId)
        AND (:fromDate IS NULL OR r.createdAt >= :fromDate)
        AND (:toDate IS NULL OR r.createdAt <= :toDate)
    """)
    Page<Return> findByFilters(
            @Param("status") String status,
            @Param("searchTerm") String searchTerm,
            @Param("productId") Long productId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT r FROM Return r WHERE " +
           "r.status IN :statuses AND " +
           "r.createdAt <= :cutoffDate")
    List<Return> findStaleReturns(
            @Param("statuses") List<String> statuses,
            @Param("cutoffDate") LocalDateTime cutoffDate);

    List<Return> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Return> findByApprovedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Return> findByCompletedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT r FROM Return r WHERE " +
           "DATE(r.createdAt) = CURRENT_DATE")
    List<Return> findTodaysReturns();

    @Query("SELECT r FROM Return r WHERE " +
           "WEEK(r.createdAt) = WEEK(CURRENT_DATE) AND " +
           "YEAR(r.createdAt) = YEAR(CURRENT_DATE)")
    List<Return> findThisWeeksReturns();

    @Query("SELECT r FROM Return r WHERE " +
           "MONTH(r.createdAt) = MONTH(CURRENT_DATE) AND " +
           "YEAR(r.createdAt) = YEAR(CURRENT_DATE)")
    List<Return> findThisMonthsReturns();

    @Query("SELECT COUNT(r) FROM Return r WHERE " +
           "r.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);


    @Query("SELECT r.reason as reason, COUNT(r) as count " +
           "FROM Return r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY r.reason " +
           "ORDER BY count DESC")
    List<Object[]> getReturnReasonDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    @Query("SELECT SUM(r.refundAmount) FROM Return r WHERE " +
           "r.status = 'REFUND_COMPLETED' AND " +
           "r.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRefundAmountByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(FUNCTION('TIMESTAMPDIFF', HOUR, r.createdAt, r.completedAt)) FROM Return r " +
           "WHERE r.completedAt IS NOT NULL AND r.completedAt BETWEEN :startDate AND :endDate")
    Double getAverageProcessingTime(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r.reason as reason, " +
           "COUNT(r) as count, " +
           "SUM(r.totalRefundAmount) as totalRefund " +
           "FROM Return r " +
           "WHERE r.createdAt BETWEEN :fromDate AND :toDate " +
           "GROUP BY r.reason " +
           "ORDER BY count DESC")
    List<Object[]> getReturnReasonAnalyticsRaw(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);
    
    @Query("""
    SELECT new com.project.backend.ResponseDto.ProductReturnStatsDto(
        oi.productId,
        oi.productName,
        COUNT(ri),
        SUM(r.totalRefundAmount)
    )
    FROM Return r
    JOIN r.items ri
    JOIN OrderItem oi ON oi.id = ri.orderItemId
    GROUP BY oi.productId, oi.productName
    ORDER BY COUNT(ri) DESC
""")
Page<ProductReturnStatsDto> getTopReturnedProducts(Pageable pageable);

    @Query("SELECT r.user.id as userId, " +
           "r.user.email as email, " +
           "COUNT(r) as returnCount, " +
           "SUM(r.refundAmount) as totalRefund " +
           "FROM Return r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY r.user.id, r.user.email " +
           "HAVING COUNT(r) > :minReturns " +
           "ORDER BY returnCount DESC")
    List<Object[]> getFrequentReturners(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minReturns") long minReturns);

    @Query("SELECT COUNT(r) FROM Return r " +
           "WHERE r.user.id = :userId")
    Long getUserReturnCount(@Param("userId") Long userId);

    @Query("SELECT DATE(r.createdAt) as date, " +
           "COUNT(r) as count, " +
           "SUM(r.refundAmount) as totalAmount " +
           "FROM Return r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(r.createdAt) " +
           "ORDER BY date")
    List<Object[]> getDailyReturnSummary(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT HOUR(r.createdAt) as hour, " +
           "COUNT(r) as count " +
           "FROM Return r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY HOUR(r.createdAt) " +
           "ORDER BY hour")
    List<Object[]> getHourlyReturnDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Modifying
    @Transactional
    @Query("UPDATE Return r SET r.status = :newStatus, " +
           "r.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE r.id = :returnId AND r.status = :currentStatus")
    int updateStatus(
            @Param("returnId") Long returnId,
            @Param("currentStatus") String currentStatus,
            @Param("newStatus") String newStatus);

    @Modifying
    @Transactional
    @Query("UPDATE Return r SET r.approvedAt = CURRENT_TIMESTAMP, " +
           "r.approvedBy = :adminId, " +
           "r.status = 'APPROVED' " +
           "WHERE r.id = :returnId AND r.status = 'PENDING_APPROVAL'")
    int markAsApproved(
            @Param("returnId") Long returnId,
            @Param("adminId") Long adminId);

            

    @Modifying
    @Transactional
    @Query("UPDATE Return r SET r.rejectedAt = CURRENT_TIMESTAMP, " +
           "r.rejectionReason = :reason, " +
           "r.status = 'REJECTED' " +
           "WHERE r.id = :returnId AND r.status = 'PENDING_APPROVAL'")
    int markAsRejected(
            @Param("returnId") Long returnId,
            @Param("reason") String reason);

    @Query("SELECT r FROM Return r WHERE " +
           "r.status = 'PICKUP_SCHEDULED' AND " +
           "r.pickupScheduledDate BETWEEN :startDate AND :endDate")
    List<Return> findPickupsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r FROM Return r WHERE " +
           "r.status = 'PICKUP_SCHEDULED' AND " +
           "r.pickupScheduledDate < :currentDate")
    List<Return> findOverduePickups(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT r.id FROM Return r WHERE " +
           "r.status = :status AND " +
           "r.createdAt < :date")
    List<Long> findReturnIdsByStatusAndCreatedBefore(
            @Param("status") String status,
            @Param("date") LocalDateTime date);

    @Modifying
    @Transactional
    @Query("UPDATE Return r SET r.status = :newStatus " +
           "WHERE r.id IN :returnIds")
    int bulkUpdateStatus(
            @Param("returnIds") List<Long> returnIds,
            @Param("newStatus") String newStatus);

    @Query(value = "SELECT " +
           "DATE(r.created_at) as date, " +
           "COUNT(CASE WHEN r.status = 'PENDING_APPROVAL' THEN 1 END) as pending, " +
           "COUNT(CASE WHEN r.status = 'APPROVED' THEN 1 END) as approved, " +
           "COUNT(CASE WHEN r.status = 'REJECTED' THEN 1 END) as rejected, " +
           "COUNT(CASE WHEN r.status = 'REFUND_COMPLETED' THEN 1 END) as completed, " +
           "SUM(CASE WHEN r.status = 'REFUND_COMPLETED' THEN r.refund_amount ELSE 0 END) as total_refund " +
           "FROM returns r " +
           "WHERE r.created_at BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(r.created_at) " +
           "ORDER BY date", nativeQuery = true)
    List<Object[]> getReturnReportData(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT NEW map(" +
           "r.reason as reason, " +
           "COUNT(r) as count, " +
           "SUM(r.refundAmount) as totalAmount, " +
           "AVG(r.refundAmount) as avgAmount) " +
           "FROM Return r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY r.reason")
    List<Object[]> getReturnReasonAnalytics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    Optional<Return> findByTrackingId(String awb);

    Page<Return> findByUserIdAndStatus(Long id, String status, Pageable pageable);
}