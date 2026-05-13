package org.delicias.shoppingcart.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShoppingRestaurantDTO(
        boolean exists,
        UUID id,
        Integer lineCount
) {
}
