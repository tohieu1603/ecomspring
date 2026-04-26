package com.hieu.cart_service.grpc.server;

import com.hieu.cart_service.entity.CartItem;
import com.hieu.cart_service.interfaces.grpc.proto.CartItemSnapshot;
import com.hieu.cart_service.interfaces.grpc.proto.CartServiceGrpc;
import com.hieu.cart_service.interfaces.grpc.proto.ClearCartRequest;
import com.hieu.cart_service.interfaces.grpc.proto.ClearCartResponse;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartRequest;
import com.hieu.cart_service.interfaces.grpc.proto.GetCartResponse;
import com.hieu.cart_service.repository.CartItemRepository;
import com.hieu.cart_service.service.CartService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

/**
 * gRPC server implementation for CartService.
 * Used by order-service to read/clear a user's cart before placing an order.
 */
@GrpcService
@RequiredArgsConstructor
public class CartGrpcService extends CartServiceGrpc.CartServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(CartGrpcService.class);

    private final CartItemRepository cartItemRepository;
    private final CartService cartService;

    @Override
    public void getCart(GetCartRequest request, StreamObserver<GetCartResponse> observer) {
        try {
            var items = cartItemRepository.findAllByUserId(request.getUserId());
            var snapshots = items.stream().map(this::toSnapshot).toList();
            var reply = GetCartResponse.newBuilder()
                .setUserId(request.getUserId())
                .addAllItems(snapshots)
                .build();
            observer.onNext(reply);
        } catch (Exception e) {
            log.error("gRPC getCart failed for user {}: {}", request.getUserId(), e.getMessage(), e);
            observer.onNext(GetCartResponse.newBuilder().setUserId(request.getUserId()).build());
        }
        observer.onCompleted();
    }

    @Override
    public void clearCart(ClearCartRequest request, StreamObserver<ClearCartResponse> observer) {
        try {
            cartService.clearCart(request.getUserId());
            observer.onNext(ClearCartResponse.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            log.error("gRPC clearCart failed for user {}: {}", request.getUserId(), e.getMessage(), e);
            observer.onNext(ClearCartResponse.newBuilder().setSuccess(false).build());
        }
        observer.onCompleted();
    }

    private CartItemSnapshot toSnapshot(CartItem item) {
        return CartItemSnapshot.newBuilder()
            .setId(item.getId() != null ? item.getId() : 0L)
            .setUserId(nz(item.getUserId()))
            .setProductId(item.getProductId() != null ? item.getProductId() : 0L)
            .setProductName(nz(item.getProductName()))
            .setVariantId(item.getVariantId() != null ? item.getVariantId() : 0L)
            .setVariantSku(nz(item.getVariantSku()))
            .setVariantImage(nz(item.getVariantImage()))
            .setUnitPrice(item.getUnitPrice() != null ? item.getUnitPrice().toPlainString() : "0")
            .setQuantity(item.getQuantity() != null ? item.getQuantity() : 0)
            .build();
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
