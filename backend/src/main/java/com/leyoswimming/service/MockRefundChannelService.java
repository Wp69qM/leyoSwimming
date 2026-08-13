package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.RefundTransaction;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundTransactionMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockRefundChannelService {

  private final RefundTransactionMapper refundTransactionMapper;
  private final OrderMapper orderMapper;
  private final PackageMapper packageMapper;

  @Async
  public void notifyRefundSuccess(String channelRefundNo) {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Mock refund callback interrupted: {}", channelRefundNo);
      return;
    }

    LambdaQueryWrapper<RefundTransaction> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(RefundTransaction::getChannelRefundNo, channelRefundNo).last("LIMIT 1");
    RefundTransaction transaction = refundTransactionMapper.selectOne(wrapper);
    if (transaction == null || !"pending".equals(transaction.getStatus())) {
      log.warn("Refund transaction not found or not pending: {}", channelRefundNo);
      return;
    }

    RefundTransaction updateTransaction = new RefundTransaction();
    updateTransaction.setId(transaction.getId());
    updateTransaction.setStatus("success");
    refundTransactionMapper.updateById(updateTransaction);

    Order order = orderMapper.selectById(transaction.getOrderId());
    if (order != null && OrderStatus.REFUND_PROCESSING.getValue().equals(order.getStatus())) {
      Order updateOrder = new Order();
      updateOrder.setId(order.getId());
      updateOrder.setStatus(OrderStatus.REFUNDED.getValue());
      updateOrder.setRefundedAt(LocalDateTime.now());
      orderMapper.updateById(updateOrder);

      if (order.getPackageId() != null) {
        UpdateWrapper<CoursePackage> packageWrapper = new UpdateWrapper<>();
        packageWrapper
            .eq("id", order.getPackageId())
            .set("status", "refunded")
            .set("refunded_at", LocalDateTime.now())
            .set("frozen_reason", null);
        packageMapper.update(packageWrapper);
      }
    }

    log.info("Mock refund callback success: orderId={}, channelRefundNo={}",
        transaction.getOrderId(), channelRefundNo);
  }
}
