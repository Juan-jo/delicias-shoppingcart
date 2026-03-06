package org.delicias.line.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;


public record UpdateShoppingCartLineDTO(
        @NotNull(message = "Id is mandatory")
        UUID id,

        @NotNull(message = "Qty is mandatory")
        Short qty,

        Set<Integer> attrValues
)
{ }
