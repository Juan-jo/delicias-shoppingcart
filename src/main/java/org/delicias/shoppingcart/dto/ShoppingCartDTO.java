package org.delicias.shoppingcart.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.delicias.common.adjusment.AdjustmentType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ShoppingCartDTO(
        UUID id,
        boolean hasDeliveryAddress,
        DeliveryAddress deliveryAddress,
        List<ShoppingLine> shoppingLines,
        BigDecimal subtotal,
        BigDecimal total,

        List<ShoppingCharge> charges,
        boolean hasPromApplied,
        PromApplied prom
) {

    @Builder
    public record ShoppingLine(
            UUID id,
            Integer productTmplId,
            String productTmplName,
            String productTmplDescription,
            Short qty,
            BigDecimal priceTotal,

            String pictureUrl,

            Set<AttrAddedItem> attrsAdded,

            @JsonIgnore
            Set<Integer> attrValuesAdded // Use For Generate Order

    ) {}

    @Builder
    public record DeliveryAddress(
            @JsonIgnore
            Integer id,

            String name,
            String address,
            //UserAddressType addressType,

            @JsonIgnore
            double latitude,
            @JsonIgnore
            double longitude
    ) {}

    @Builder
    public record ShoppingCharge(
            String name,
            AdjustmentType adjustmentType,
            double amount
    ) { }

    @Builder
    public record PromApplied(
            String name,
            String desc
    ){}

    @Builder
    public record AttrAddedItem(
            String attrName,
            String values
    ) {}

}
