package org.delicias.line.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AddShoppingCartLineDTO(

        @NotNull(message = "Product template Id is mandatory")
        Integer productTmplId,

        @NotNull(message = "Qty is mandatory")
        Short qty,

        Set<Integer> attrValues
) { }
