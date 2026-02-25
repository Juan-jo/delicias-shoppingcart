package org.delicias.shoppingcart.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.delicias.common.adjusment.AdjustmentKeys;
import org.delicias.common.adjusment.AdjustmentType;
import org.delicias.common.adjusment.OrderAdjustment;
import org.delicias.common.dto.product.ProductPriceDTO;
import org.delicias.common.dto.restaurant.RestaurantLatLngDTO;
import org.delicias.common.dto.restaurant.RestaurantResumeDTO;
import org.delicias.common.dto.user.DefaultAddressDTO;
import org.delicias.common.dto.user.UserShoppingAddressDTO;
import org.delicias.exception.ShoppingBusinessException;
import org.delicias.exception.ShoppingErrorCode;
import org.delicias.line.domain.model.ShoppingCartLine;
import org.delicias.line.domain.repository.ShoppingCartLineRepository;
import org.delicias.rest.clients.ProductClient;
import org.delicias.rest.clients.RestaurantClient;
import org.delicias.rest.clients.UserClient;
import org.delicias.rest.security.SecurityContextService;
import org.delicias.shoppingcart.domain.model.ShoppingCart;
import org.delicias.shoppingcart.domain.repository.ShippingCostService;
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

    @Inject
    @RestClient
    UserClient userClient;

    @Inject
    ShippingCostService shippingCostService;

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




    @Transactional
    public ShoppingCartDTO findById(UUID shoppingCartId) {

        ShoppingCart shoppingCart = cartRepository.findById(shoppingCartId);

        if (shoppingCart == null) {
            throw new NotFoundException("ShoppingCart Not Found");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        List<ShoppingCartDTO.ShoppingLine> lines = new ArrayList<>();
        List<ShoppingCartLine> linesAdded = lineRepository.getByShoppingCart(shoppingCartId);

        Map<Integer, ProductPriceDTO> productsMap = getProductPrices(
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

        DeliveryAddressResult deliveryAddress = resolveDeliveryAddress(shoppingCart);

        List<ShoppingCartDTO.ShoppingCharge> charges =
                getShoppingCharges(shoppingCart.getAdjustments());


        BigDecimal totalCharges = charges.stream()
                .map(i->BigDecimal.valueOf(i.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ShoppingCartDTO.builder()
                .id(shoppingCart.getId())
                .shoppingLines(lines)
                .charges(charges)
                .hasDeliveryAddress(deliveryAddress.hasDeliveryAddress)
                .deliveryAddress(deliveryAddress.deliveryAddress)
                .subtotal(subtotal)
                .total(subtotal.add(totalCharges))
                .build();
    }



    private List<ProductPriceDTO> getProductPrices(Set<Integer> ids) {

        try (Response response = productClient.getProductTmplPrices(ids)) {

            if(response.getStatus() == Response.Status.PARTIAL_CONTENT.getStatusCode()) {

                throw new ShoppingBusinessException(
                        "Can't get prices",
                        ShoppingErrorCode.SHOPPING_BAD_PRICES,
                        400
                );
            }
            return response.readEntity(new GenericType<>() {});
        }
    }

    private RestaurantLatLngDTO getRestaurantLatLng(Integer restaurantTmplId) {

        RestaurantLatLngDTO restaurant = restaurantClient.getLatLng(restaurantTmplId);

        if(restaurant.latitude().equals(Double.NaN) || restaurant.longitude().equals(Double.NaN)) {
            throw new ShoppingBusinessException(
                    "Can't get restaurant position",
                    ShoppingErrorCode.SHOPPING_BAD_RESTAURANT_LAT_LNG,
                    400
            );
        }

        return restaurant;
    }

    private DefaultAddressDTO getUserAddressDefault() {

        try (Response response = userClient.getUserAddressDefault()) {

            if(response.getStatus() == Response.Status.PARTIAL_CONTENT.getStatusCode()) {

                throw new ShoppingBusinessException(
                        "Can't get default user address",
                        ShoppingErrorCode.SHOPPING_BAD_USER_ADDRESS_DEFAULT,
                        400
                );
            }
            return response.readEntity(DefaultAddressDTO.class);
        }
    }

    private UserShoppingAddressDTO getShoppingAddress(Integer addressId) {

        try (Response response = userClient.getUserAddress(addressId)) {

            if(response.getStatus() == Response.Status.PARTIAL_CONTENT.getStatusCode()) {

                throw new ShoppingBusinessException(
                        "Can't get shopping address",
                        ShoppingErrorCode.SHOPPING_BAD_USER_ADDRESS_DEFAULT,
                        400
                );
            }
            return response.readEntity(UserShoppingAddressDTO.class);
        }
    }


    private static List<ShoppingCartDTO.ShoppingCharge> getShoppingCharges(List<OrderAdjustment> adjustments) {
        List<ShoppingCartDTO.ShoppingCharge> charges;
        charges = Optional.ofNullable(adjustments)
                .orElseGet(List::of)
                .stream()
                .map(adjustment -> ShoppingCartDTO.ShoppingCharge.builder()
                        .adjustmentType(adjustment.getType())
                        .name(adjustment.getName())
                        .amount(adjustment.getAmount())
                        .build())
                .toList();
        return charges;
    }

    private DeliveryAddressResult resolveDeliveryAddress(ShoppingCart shoppingCart) {

        if(shoppingCart.getUserAddressId() != null) {

            UserShoppingAddressDTO address = getShoppingAddress(shoppingCart.getUserAddressId());

            return new DeliveryAddressResult(true,
                    ShoppingCartDTO.DeliveryAddress.builder()
                            .name(address.name())
                            .address(address.address())
                            .addressType(address.addressType())
                            .build()
            );
        }

        DefaultAddressDTO defaultAddress = getUserAddressDefault();

        if(defaultAddress.exists()) {

            RestaurantLatLngDTO restaurantLatLng  = getRestaurantLatLng(shoppingCart.getRestaurantTmplId());

            Integer distance = cartRepository.getDistance(
                    defaultAddress.longitude(), defaultAddress.latitude(),
                    restaurantLatLng.longitude(), restaurantLatLng.latitude()
            );

            double shipmentCost = shippingCostService.calculate(distance);

            shoppingCart.addAdjustment(OrderAdjustment.builder()
                    .key(AdjustmentKeys.SHIPPING_COST)
                    .amount(shipmentCost)
                    .type(AdjustmentType.CHARGE)
                    .name("Costo de envío")
                    .build());

            shoppingCart.setUserAddressId(defaultAddress.data().id());

            return new DeliveryAddressResult(true, ShoppingCartDTO.DeliveryAddress.builder()
                    .name(defaultAddress.data().name())
                    .address(defaultAddress.data().address())
                    .addressType(defaultAddress.data().addressType())
                    .build()
            );
        }

        return new DeliveryAddressResult(false, null);

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
                .productTmplId(line.getProductTmplId())
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

    private record DeliveryAddressResult(
            boolean hasDeliveryAddress,
            ShoppingCartDTO.DeliveryAddress deliveryAddress
    ) {}
}
