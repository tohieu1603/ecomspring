package com.hieu.cart_service.service;

import com.hieu.cart_service.dto.AddToCartRequest;
import com.hieu.cart_service.dto.CartDTO;
import com.hieu.cart_service.dto.CartItemDTO;
import com.hieu.cart_service.dto.UpdateCartItemRequest;
import com.hieu.cart_service.entity.CartItem;
import com.hieu.cart_service.exception.CartItemNotFoundException;
import com.hieu.cart_service.grpc.client.CatalogGrpcClient;
import com.hieu.cart_service.redis.CartCacheService;
import com.hieu.cart_service.repository.CartItemRepository;
import com.hieu.catalog_service.interfaces.grpc.proto.GetProductResponse;
import com.hieu.catalog_service.interfaces.grpc.proto.Variant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application service for cart operations.
 * Coordinates repository, Redis cache and catalog gRPC client.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final CartCacheService cacheService;
    private final Optional<CatalogGrpcClient> catalogClient;

    // ── READ ─────────────────────────────────────────────────────────────────

    /**
     * Fetches cart for user; uses Redis cache first.
     * Revalidates each item via catalog gRPC and attaches warnings when unavailable.
     */
    @Transactional(readOnly = true)
    public CartDTO getCart(String userId) {
        var cached = cacheService.getCart(userId);
        if (cached != null) {
            return cached;
        }
        var items = cartItemRepository.findAllByUserId(userId);
        var cart = buildCartDTO(userId, items, true);
        cacheService.putCart(userId, cart);
        return cart;
    }

    // ── WRITE ────────────────────────────────────────────────────────────────

    /**
     * Adds or updates a cart item, with idempotency check via Redis.
     */
    @Transactional
    public CartDTO addItem(String userId, AddToCartRequest req) {
        // Idempotency guard
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            var idem = cacheService.getIdempotentResult(req.idempotencyKey());
            if (idem != null) return idem;
        }

        // Resolve variant from catalog (caller passes productId to avoid an extra round-trip).
        var variantOpt = catalogClient.flatMap(c -> c.getVariantById(req.productId(), req.variantId()));

        CartItem item;
        var existing = cartItemRepository.findByUserIdAndVariantId(userId, req.variantId());
        if (existing.isPresent()) {
            item = existing.get();
            item.setQuantity(item.getQuantity() + req.quantity());
            if (item.getQuantity() > 999) item.setQuantity(999);
        } else {
            item = CartItem.builder()
                .userId(userId)
                .variantId(req.variantId())
                .productId(variantOpt.map(v -> v.getProductId()).orElse(0L))
                .productName(resolveProductName(req.variantId(), variantOpt))
                .variantSku(variantOpt.map(Variant::getSku).orElse("unknown"))
                .variantImage(null)
                .unitPrice(variantOpt.map(v -> parseBD(v.getPrice())).orElse(BigDecimal.ZERO))
                .quantity(req.quantity())
                .build();
        }
        // Refresh denormalised fields from catalog if available
        variantOpt.ifPresent(v -> {
            item.setUnitPrice(parseBD(v.getPrice()));
            item.setVariantSku(v.getSku());
        });

        cartItemRepository.save(item);
        cacheService.evictCart(userId);

        var cart = buildCartDTO(userId, cartItemRepository.findAllByUserId(userId), false);
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            cacheService.putIdempotentResult(req.idempotencyKey(), cart);
        }
        cacheService.putCart(userId, cart);
        return cart;
    }

    /**
     * Updates quantity of an existing cart item. quantity=0 means delete.
     */
    @Transactional
    public CartDTO updateItem(String userId, Long variantId, UpdateCartItemRequest req) {
        if (req.quantity() == 0) {
            return removeItem(userId, variantId);
        }
        var item = cartItemRepository.findByUserIdAndVariantId(userId, variantId)
            .orElseThrow(() -> new CartItemNotFoundException(userId, variantId));
        item.setQuantity(req.quantity());
        cartItemRepository.save(item);
        cacheService.evictCart(userId);
        return refreshAndCache(userId);
    }

    /** Deletes a single cart item. */
    @Transactional
    public CartDTO removeItem(String userId, Long variantId) {
        cartItemRepository.deleteByUserIdAndVariantId(userId, variantId);
        cacheService.evictCart(userId);
        return refreshAndCache(userId);
    }

    /** Clears the entire cart for a user. */
    @Transactional
    public void clearCart(String userId) {
        cartItemRepository.deleteAllByUserId(userId);
        cacheService.evictCart(userId);
    }

    // ── PACKAGE/INTERNAL ─────────────────────────────────────────────────────

    /** Called by Kafka consumer: remove items by productId and evict affected caches. */
    @Transactional
    public void removeItemsByProduct(Long productId) {
        var affected = cartItemRepository.findUserIdsByProductId(productId);
        cartItemRepository.deleteAllByProductId(productId);
        affected.forEach(cacheService::evictCart);
        log.info("Removed cart items for productId={}, affected users={}", productId, affected.size());
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private CartDTO refreshAndCache(String userId) {
        var items = cartItemRepository.findAllByUserId(userId);
        var cart = buildCartDTO(userId, items, false);
        cacheService.putCart(userId, cart);
        return cart;
    }

    private CartDTO buildCartDTO(String userId, List<CartItem> items, boolean revalidate) {
        var warnings = new ArrayList<String>();
        var dtoItems = items.stream().map(item -> {
            String warning = null;
            if (revalidate) {
                warning = revalidateItem(item);
                if (warning != null) warnings.add(warning);
            }
            var subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemDTO(
                item.getId(), item.getProductId(), item.getProductName(),
                item.getVariantId(), item.getVariantSku(), item.getVariantImage(),
                item.getUnitPrice(), item.getQuantity(), subtotal,
                warning, item.getUpdatedAt());
        }).toList();

        var totalAmount = dtoItems.stream()
            .map(CartItemDTO::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDTO(userId, dtoItems, dtoItems.size(), totalAmount, warnings);
    }

    /** Returns a warning string if the item's variant is unavailable/price-changed, or null. */
    private String revalidateItem(CartItem item) {
        if (catalogClient.isEmpty()) return null;
        var respOpt = catalogClient.get().getVariantBySku(item.getVariantSku());
        if (respOpt.isEmpty()) return "unavailable: catalog unreachable for sku=" + item.getVariantSku();
        var resp = respOpt.get();
        if (!resp.getFound()) return "unavailable: sku=" + item.getVariantSku() + " not found";
        var v = resp.getVariant();
        if (!"ACTIVE".equalsIgnoreCase(v.getStatus())) return "inactive: sku=" + item.getVariantSku();
        var catalogPrice = parseBD(v.getPrice());
        if (catalogPrice.compareTo(item.getUnitPrice()) != 0) {
            return "price changed: sku=%s was=%s now=%s".formatted(item.getVariantSku(), item.getUnitPrice(), catalogPrice);
        }
        return null;
    }

    /**
     * Resolves productId for a variant — attempts catalog lookup by scanning the variant.
     * Falls back to 0L if catalog is unavailable.
     */

    private String resolveProductName(Long variantId, Optional<Variant> variantOpt) {
        // product name not on Variant proto — requires GetProduct; skip for simplicity
        return variantOpt.map(v -> "Product-" + v.getProductId()).orElse("Unknown");
    }

    private static BigDecimal parseBD(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
