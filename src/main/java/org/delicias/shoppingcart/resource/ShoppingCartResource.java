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

}
