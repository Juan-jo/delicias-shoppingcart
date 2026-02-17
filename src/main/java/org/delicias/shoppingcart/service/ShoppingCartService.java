package org.delicias.shoppingcart.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.delicias.common.dto.product.ProductPriceDTO;
import org.delicias.common.dto.restaurant.RestaurantResumeDTO;
import org.delicias.line.domain.model.ShoppingCartLine;
import org.delicias.line.domain.repository.ShoppingCartLineRepository;
import org.delicias.rest.clients.ProductClient;
import org.delicias.rest.clients.RestaurantClient;
import org.delicias.rest.security.SecurityContextService;
import org.delicias.shoppingcart.domain.model.ShoppingCart;
import org.delicias.shoppingcart.domain.repository.ShoppingCartRepository;
import org.delicias.shoppingcart.dto.ShoppingCartAvailableDTO;
import org.delicias.shoppingcart.dto.ShoppingCartDTO;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@ApplicationScoped
public class ShoppingCartService {

    @Inject
    SecurityContextService security;

    @Inject
    ShoppingCartRepository cartRepository;

    @Inject
    ShoppingCartLineRepository lineRepository;

    @Inject
    @RestClient
    RestaurantClient restaurantClient;

    @Inject
    @RestClient
    ProductClient productClient;

    public List<ShoppingCartAvailableDTO> cartsAvailable() {

        var shoppingCarts = cartRepository.findByUser(UUID.fromString(security.userId()));

        if (shoppingCarts.isEmpty()) {
            return List.of();
        }

        Map<Integer, RestaurantResumeDTO> restaurantsMap =
                restaurantClient.getRestaurantsByIds(
                                shoppingCarts.stream().map(ShoppingCart::getRestaurantTmplId).collect(Collectors.toSet())
                        )
                        .stream()
                        .collect(Collectors.toMap(RestaurantResumeDTO::id, r -> r));

        return shoppingCarts.stream().map(it -> {

            RestaurantResumeDTO resumeDTO = restaurantsMap.get(it.getRestaurantTmplId());

            if (resumeDTO == null) {
                return null;
            }

            return ShoppingCartAvailableDTO.builder()
                    .id(it.getId())
                    .restaurantName(resumeDTO.name())
                    .restaurantLogo(resumeDTO.logoUrl())
                    .lineCount(it.getLineCount())
                    .build();

        }).filter(Objects::nonNull).toList();
    }

    public ShoppingCartDTO findById(UUID shoppingCartId) {

        ShoppingCart shoppingCart = cartRepository.findById(shoppingCartId);

        if (shoppingCart == null) {
            throw new NotFoundException("ShoppingCart Not Found");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        List<ShoppingCartDTO.ShoppingLine> lines = new ArrayList<>();
        List<ShoppingCartLine> linesAdded = lineRepository.getByShoppingCart(shoppingCartId);

        Map<Integer, ProductPriceDTO> productsMap = findProducts(
                linesAdded.stream().map(ShoppingCartLine::getProductTmplId).collect(Collectors.toSet())
        )
                .stream()
                .collect(Collectors.toMap(ProductPriceDTO::productTmplId, p -> p));

        for(ShoppingCartLine line: linesAdded) {

            ProductPriceDTO product = productsMap.get(line.getProductTmplId());

            if(product != null) {

                AttrCalculationResult attrResult = calculateAttributes(line, product);

                BigDecimal basePrice = Optional.ofNullable(product.listPrice()).orElse(BigDecimal.ZERO);

                BigDecimal lineTotal = basePrice.multiply(BigDecimal.valueOf(line.getQty()))
                        .add(attrResult.extraPrice());

                lines.add(buildShoppingLine(line, lineTotal, attrResult, product));

                subtotal = subtotal.add(lineTotal);
            }

        }

        return ShoppingCartDTO.builder()
                .id(shoppingCart.getId())
                .shoppingLines(lines)
                .subtotal(subtotal)
                .total(BigDecimal.ZERO)
                .build();
    }



    private List<ProductPriceDTO> findProducts(Set<Integer> ids) {

        try (Response response = productClient.getProductTmplPrices(ids)) {

            return response.readEntity(new GenericType<>() {});

        } catch (WebApplicationException e) {

            Response errorResponse = e.getResponse();

            throw new RuntimeException("El servicio de productos devolvió un error: " + errorResponse.getStatus());
        } catch (ProcessingException e) {

            throw new RuntimeException("Error técnico de comunicación o formato de datos", e);
        }
    }

    private AttrCalculationResult calculateAttributes(ShoppingCartLine line, ProductPriceDTO product) {

        Set<Integer> selectedIds = line.getAttrValuesIds() != null ?
                new HashSet<>(line.getAttrValuesIds()) : Collections.emptySet();

        BigDecimal extraPrice = product.attributes().stream()
                .flatMap(attr -> attr.values().stream())
                .filter(v -> selectedIds.contains(v.attrValueId()))
                .map(v -> Optional.ofNullable(v.extraPrice()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(line.getQty()));


        Set<ShoppingCartDTO.AttrAddedItem> attrsAdded = product.attributes().stream()
                .map(attr -> {
                    String selectedValuesNames = attr.values().stream()
                            .filter(v -> selectedIds.contains(v.attrValueId()))
                            .map(ProductPriceDTO.AttributeValueDTO::name)
                            .collect(Collectors.joining(", "));

                    return new AbstractMap.SimpleEntry<>(attr.name(), selectedValuesNames);
                })
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> new ShoppingCartDTO.AttrAddedItem(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new AttrCalculationResult(extraPrice, selectedIds, attrsAdded);
    }

    private ShoppingCartDTO.ShoppingLine buildShoppingLine(
            ShoppingCartLine line,
            BigDecimal lineTotal,
            AttrCalculationResult attrResult,
            ProductPriceDTO product
    ) {
        return ShoppingCartDTO.ShoppingLine.builder()
                .id(line.getId())
                .productTmplName(product.name())
                .productTmplDescription(product.description())
                .qty(line.getQty())
                .priceTotal(lineTotal)
                .attrsAdded(attrResult.attrsAdded())
                .pictureUrl(product.pictureUrl())
                .build();
    }


    private record AttrCalculationResult(
            BigDecimal extraPrice,
            Set<Integer> selectedIds,
            Set<ShoppingCartDTO.AttrAddedItem> attrsAdded
    ) {}
}
