package org.delicias.line.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShoppingCartLineDTO(
        UUID id,
        Integer productTmplId,
        Short qty,
        UUID shoppingCartId,
        Set<Integer> attrValues
)
{ }
