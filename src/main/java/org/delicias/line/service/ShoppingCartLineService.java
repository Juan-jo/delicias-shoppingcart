package org.delicias.line.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.delicias.common.dto.product.ProductCandidateShoppingLineDTO;
import org.delicias.line.domain.model.ShoppingCartLine;
import org.delicias.line.domain.repository.ShoppingCartLineRepository;
import org.delicias.line.dto.AddShoppingCartLineDTO;
import org.delicias.rest.clients.ProductClient;
import org.delicias.rest.security.SecurityContextService;
import org.delicias.shoppingcart.domain.model.ShoppingCart;
import org.delicias.shoppingcart.domain.repository.ShoppingCartRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ShoppingCartLineService {

    @Inject
    ShoppingCartRepository shoppingRepository;

    @Inject
    ShoppingCartLineRepository lineRepository;

    @Inject
    SecurityContextService security;

    @Inject
    @RestClient
    ProductClient productClient;

    @Transactional
    public void addLine(AddShoppingCartLineDTO req) {

        ProductCandidateShoppingLineDTO lineDTO = findProduct(req.productTmplId());


        if (req.attrValues() != null && !req.attrValues().isEmpty()) {
            Set<Integer> validIds = lineDTO.attrValues().stream()
                    .map(ProductCandidateShoppingLineDTO.AttributeValueDTO::attrValueId)
                    .collect(Collectors.toSet());

            boolean allValid = validIds.containsAll(req.attrValues());

            if (!allValid) {
                throw new BadRequestException("Uno o más atributos seleccionados no pertenecen a este producto.");
            }
        }

        UUID userUUID = UUID.fromString(security.userId());

        ShoppingCart shoppingCart = shoppingRepository.findByUserAndRestaurant(userUUID, lineDTO.restaurantTmplId())
                .orElseGet(() -> {
                    ShoppingCart newCart = ShoppingCart.builder()
                            .userUUID(userUUID)
                            .restaurantTmplId(lineDTO.restaurantTmplId())
                            .build();
                    shoppingRepository.persist(newCart);
                    return newCart;
                });


        lineRepository.persist(
                ShoppingCartLine.builder()
                        .shoppingCart(shoppingCart)
                        .productTmplId(req.productTmplId())
                        .qty(req.qty())
                        .attrValuesIds(req.attrValues())
                        .build()
        );

    }

    public ProductCandidateShoppingLineDTO findProduct(Integer id) {
        try (Response response = productClient.getProductCandidateById(id)) {
            return response.readEntity(ProductCandidateShoppingLineDTO.class);

        } catch (WebApplicationException e) {

            Response errorResponse = e.getResponse();

            if (errorResponse.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException("El producto con ID " + id + " no existe en el catálogo.");
            }


            throw new RuntimeException("El servicio de productos devolvió un error: " + errorResponse.getStatus());
        } catch (ProcessingException e) {

            throw new RuntimeException("Error técnico de comunicación o formato de datos", e);
        }
    }

}
