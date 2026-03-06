package org.delicias.line.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.delicias.line.dto.AddShoppingCartLineDTO;
import org.delicias.line.dto.UpdateShoppingCartLineDTO;
import org.delicias.line.service.ShoppingCartLineService;

import java.util.Map;
import java.util.UUID;

@Authenticated
@Path("/api/shoppingcart/line")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ShoppingCartLineResource {

    @Inject
    ShoppingCartLineService service;


    @POST
    public Response addLine(@Valid AddShoppingCartLineDTO req) {

        service.addLine(req);
        return Response.ok().build();
    }

    @GET
    @Path("/{cartLineId}")
    public Response findById(
            @PathParam("cartLineId") UUID cartLineId
    ) {
        return Response.ok(
                service.findById(cartLineId)
        ).build();
    }

    @PATCH
    @Path("/{cartLineId}")
    public Response patchLine(
            @PathParam("cartLineId") UUID cartLineId,
            Map<String, Object> updates) {

        service.path(cartLineId, updates);
        return Response.noContent().build();
    }

    @PUT
    public Response putLine(@Valid UpdateShoppingCartLineDTO req) {

        service.updateLine(req);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{cartLineId}")
    public Response deleteById(
            @PathParam("cartLineId") UUID cartLineId
    ) {
        service.deleteById(cartLineId);
        return Response.noContent().build();
    }

}
