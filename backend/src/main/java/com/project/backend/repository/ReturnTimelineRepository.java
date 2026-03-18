package com.project.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.ReturnStatus;
import com.project.backend.entity.ReturnTimeline;

@Repository
public interface ReturnTimelineRepository extends JpaRepository<ReturnTimeline, Long> {


    List<ReturnTimeline> findByReturnRecordIdOrderByCreatedAtDesc(Long returnId);

    Page<ReturnTimeline> findByReturnRecordId(Long returnId, Pageable pageable);

    List<ReturnTimeline> findByReturnRecordIdAndIsCustomerVisibleTrueOrderByCreatedAtDesc(Long returnId);

    List<ReturnTimeline> findByReturnRecordIdAndIsAdminVisibleTrueOrderByCreatedAtDesc(Long returnId);

    List<ReturnTimeline> findByCreatedBy(String createdBy);

    List<ReturnTimeline> findByStatus(ReturnStatus status);


    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.returnRecord.id = :returnId AND " +
           "(:status IS NULL OR rt.status = :status) AND " +
           "(:createdBy IS NULL OR rt.createdBy = :createdBy) AND " +
           "(:fromDate IS NULL OR rt.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR rt.createdAt <= :toDate)")
    List<ReturnTimeline> findByFilters(
            @Param("returnId") Long returnId,
            @Param("status") ReturnStatus status,
            @Param("createdBy") String createdBy,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.returnRecord.id = :returnId AND " +
           "rt.status = :status " +
           "ORDER BY rt.createdAt DESC")
    List<ReturnTimeline> findLatestByReturnIdAndStatus(
            @Param("returnId") Long returnId,
            @Param("status") ReturnStatus status,
            Pageable pageable);


    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.returnRecord.id = :returnId " +
           "ORDER BY rt.createdAt DESC")
    List<ReturnTimeline> findLatestTimeline(@Param("returnId") Long returnId, Pageable pageable);

    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.returnRecord.id = :returnId AND " +
           "rt.createdAt = (SELECT MAX(t.createdAt) FROM ReturnTimeline t WHERE t.returnRecord.id = :returnId)")
    ReturnTimeline findLastEvent(@Param("returnId") Long returnId);


    List<ReturnTimeline> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "DATE(rt.createdAt) = CURRENT_DATE")
    List<ReturnTimeline> findTodaysTimeline();

    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.createdAt >= :since")
    List<ReturnTimeline> findRecentTimeline(@Param("since") LocalDateTime since);


    @Query("SELECT rt.status as status, COUNT(rt) as count " +
           "FROM ReturnTimeline rt " +
           "WHERE rt.returnRecord.id = :returnId " +
           "GROUP BY rt.status")
    List<Map<String, Object>> getTimelineSummary(@Param("returnId") Long returnId);

    @Query("SELECT DATE(rt.createdAt) as date, COUNT(rt) as events " +
           "FROM ReturnTimeline rt " +
           "WHERE rt.returnRecord.id = :returnId " +
           "GROUP BY DATE(rt.createdAt) " +
           "ORDER BY date")
    List<Map<String, Object>> getTimelineByDate(@Param("returnId") Long returnId);

    @Query("SELECT rt.createdBy as user, COUNT(rt) as count " +
           "FROM ReturnTimeline rt " +
           "WHERE rt.returnRecord.id = :returnId " +
           "GROUP BY rt.createdBy")
    List<Map<String, Object>> getTimelineByUser(@Param("returnId") Long returnId);


    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.returnRecord.id = :returnId AND " +
           "rt.isCustomerVisible = true " +
           "ORDER BY rt.createdAt DESC")
    List<ReturnTimeline> findCustomerVisibleTimeline(@Param("returnId") Long returnId);

    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.returnRecord.id = :returnId AND " +
           "rt.isAdminVisible = true " +
           "ORDER BY rt.createdAt DESC")
    List<ReturnTimeline> findAdminVisibleTimeline(@Param("returnId") Long returnId);


    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.returnRecord.id = :returnId AND " +
           "(LOWER(rt.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(rt.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<ReturnTimeline> searchTimeline(
            @Param("returnId") Long returnId,
            @Param("searchTerm") String searchTerm);


    @Modifying
    @Transactional
    @Query("DELETE FROM ReturnTimeline rt WHERE rt.returnRecord.id = :returnId")
    void deleteByReturnId(@Param("returnId") Long returnId);

    @Modifying
    @Transactional
    @Query("UPDATE ReturnTimeline rt SET rt.isCustomerVisible = :visible " +
           "WHERE rt.returnRecord.id = :returnId AND rt.status = :status")
    int updateCustomerVisibility(
            @Param("returnId") Long returnId,
            @Param("status") ReturnStatus status,
            @Param("visible") Boolean visible);


    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.returnRecord.id = :returnId AND " +
           "rt.metadata IS NOT NULL")
    List<ReturnTimeline> findTimelineWithMetadata(@Param("returnId") Long returnId);

    @Query("SELECT rt FROM ReturnTimeline rt WHERE " +
           "rt.metadata LIKE %:keyword%")
    List<ReturnTimeline> findByMetadataKeyword(@Param("keyword") String keyword);


    @Query("SELECT COUNT(rt) FROM ReturnTimeline rt WHERE rt.returnRecord.id = :returnId")
    Long countTimelineEntries(@Param("returnId") Long returnId);

    @Query("SELECT MIN(rt.createdAt) as firstEvent, MAX(rt.createdAt) as lastEvent " +
           "FROM ReturnTimeline rt WHERE rt.returnRecord.id = :returnId")
    Map<String, Object> getTimelineRange(@Param("returnId") Long returnId);

   
}