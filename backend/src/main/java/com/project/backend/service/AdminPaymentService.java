package com.project.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.Order;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.PaymentTransaction;
import com.project.backend.entity.Refund;
import com.project.backend.entity.RefundStatus;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.BusinessException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.PaymentTransactionRepository;
import com.project.backend.repository.RefundRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.AdminPaymentStatsDto;
import com.project.backend.requestDto.AdminPaymentTransactionDto;
import com.project.backend.requestDto.AdminRefundDto;
import com.project.backend.requestDto.DailyPaymentSummary;
import com.project.backend.requestDto.HourlyPaymentTrend;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.PaymentFilterRequest;
import com.project.backend.requestDto.ProcessRefundRequest;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;
}
