package com.project.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.OrderReturn;
import com.project.backend.entity.ReturnStatus;

@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {

	Page<OrderReturn> findByOrderItem_Order_User_Id(Long userId, Pageable pageable);

	@Query("""
			    SELECT r FROM OrderReturn r
			    WHERE (:userId IS NULL OR r.orderItem.order.user.id = :userId)
			      AND (:status IS NULL OR r.status = :status)
			""")
	Page<OrderReturn> search(@Param("userId") Long userId, @Param("status") ReturnStatus status, Pageable pageable);

	@Query("""
			    SELECT r.reason, COUNT(r)
			    FROM OrderReturn r
			    GROUP BY r.reason
			""")
	List<Object[]> countByReason();

	Page<OrderReturn> findByStatus(ReturnStatus status, Pageable pageable);

	Page<OrderReturn> findByOrderItem_Order_User_IdAndStatus(Long userId, ReturnStatus status, Pageable pageable);

	@Query("""
			    SELECT DATE(r.requestedAt), COUNT(r)
			    FROM OrderReturn r
			    GROUP BY DATE(r.requestedAt)
			    ORDER BY DATE(r.requestedAt)
			""")
	Page<Object[]> returnTrend(Pageable pageable);

	@Query("""
			    SELECT r.orderItem.product.id,
			           r.orderItem.product.name,
			           COUNT(r)
			    FROM OrderReturn r
			    GROUP BY r.orderItem.product.id, r.orderItem.product.name
			    ORDER BY COUNT(r) DESC
			""")
	Page<Object[]> topReturnedProducts(Pageable pageable);

	@Query("""
			    SELECT r.reason, COUNT(r)
			    FROM OrderReturn r
			    GROUP BY r.reason
			""")
	Page<Object[]> countByReason(Pageable pageable);

}
