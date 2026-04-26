package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

/** Full shipping address. Country defaults to Vietnam. */
public record ShippingAddress(
        String street,
        String ward,
        String district,
        String city,
        String country,
        String postalCode) {

    public ShippingAddress {
        Objects.requireNonNull(street,   "street");
        Objects.requireNonNull(ward,     "ward");
        Objects.requireNonNull(district, "district");
        Objects.requireNonNull(city,     "city");
        country = (country == null || country.isBlank()) ? "Vietnam" : country;
    }

    public static ShippingAddress of(String street, String ward, String district, String city,
                                     String country, String postalCode) {
        return new ShippingAddress(street, ward, district, city, country, postalCode);
    }
}
