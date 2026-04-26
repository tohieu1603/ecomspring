package com.hieu.user_profile_service.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddressDTO {
    Long id;
    String userId;
    String label;
    String recipientName;
    String recipientPhone;
    String street;
    String ward;
    String district;
    String city;
    String country;
    String postalCode;
    boolean isDefault;
}
