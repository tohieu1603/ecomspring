package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.command.order.CancelOrderCommand;
import com.hieu.order_service.application.common.CommandHandler;
import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.saga.OrderSagaOrchestrator;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderHandler implements CommandHandler<CancelOrderCommand, OrderDTO> {

    private final OrderRepository orderRepository;
    private final OrderDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;
    private final OrderSagaOrchestrator saga;

    @Override
    public OrderDTO handle(CancelOrderCommand cmd) {
        // executeCancelOrderSaga is itself @Transactional(REQUIRES_NEW); wrapping it in
        // an outer transaction here would suspend that inner TX and leave changes
        // uncommitted until this method returns — defeating the REQUIRES_NEW semantics.
        return saga.executeCancelOrderSaga(cmd.orderId(), cmd.reason(), cmd.requestingUserId(), cmd.isAdmin());
    }
}
