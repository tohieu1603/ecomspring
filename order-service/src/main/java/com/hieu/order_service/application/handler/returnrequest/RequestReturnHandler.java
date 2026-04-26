package com.hieu.order_service.application.handler.returnrequest;

import com.hieu.order_service.application.command.returnrequest.RequestReturnCommand;
import com.hieu.order_service.application.common.CommandHandler;
import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.application.dto.ReturnRequestDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.domain.exception.InvalidOrderStateException;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.ReturnRequest;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.domain.repository.OrderRepository;
import com.hieu.order_service.domain.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestReturnHandler implements CommandHandler<RequestReturnCommand, ReturnRequestDTO> {

    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReturnRequestDTO handle(RequestReturnCommand cmd) {
        var order = orderRepository.findById(OrderId.of(cmd.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(cmd.orderId()));
        if (!order.canBeReturned()) {
            throw new InvalidOrderStateException("Order cannot be returned in state: " + order.getStatus());
        }

        var rr = ReturnRequest.create(
                OrderId.of(cmd.orderId()),
                UserId.of(cmd.userId()),
                ReturnReason.of(cmd.reason()),
                ReturnType.valueOf(cmd.returnType()),
                cmd.images()
        );

        var saved = returnRequestRepository.save(rr);
        saved.raiseCreatedEvent();
        eventPublisher.publishEventsOf(saved);
        return mapper.toReturnDto(saved);
    }
}
