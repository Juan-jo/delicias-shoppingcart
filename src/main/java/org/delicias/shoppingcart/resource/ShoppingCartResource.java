package org.delicias.shoppingcart.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.delicias.shoppingcart.service.ShoppingCartService;

import java.util.UUID;


@Authenticated
@Path("/api/shoppingcart")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ShoppingCartResource {

    @Inject
    ShoppingCartService service;

    @GET
    public Response cartsAvailable() {
        return Response.ok(
                service.cartsAvailable()
        ).build();
    }

    @GET
    @Path("/{shoppingCartId}")
    public Response findById(
            @PathParam("shoppingCartId") UUID shoppingCartId
    ) {
        return Response.ok(
                service.findById(shoppingCartId)
        ).build();
    }

    @GET
    @Path("/restuarant/{restaurantTmplId}")
    public Response shoppingRestaurant(
            @PathParam("restaurantTmplId") Integer restaurantTmplId
    ) {
        return Response.ok(
                service.shoppingRestaurant(restaurantTmplId)
        ).build();
    }


    // TODO For Core Client
    @GET
    @Path("/{shoppingCartId}/candidate-order")
    public Response findCandidate(
            @PathParam("shoppingCartId") UUID shoppingCartId
    ) {
        return Response.ok(
                service.getCandidateOrder(shoppingCartId)
        ).build();
    }

    // TODO For Core Client
    @DELETE
    @Path("/{shoppingCartId}")
    public Response deleteById(
            @PathParam("shoppingCartId") UUID shoppingCartId
    ) {

        service.deleteByUUID(shoppingCartId);
        return Response.noContent().build();
    }

}
