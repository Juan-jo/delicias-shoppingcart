package org.delicias.line.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.delicias.line.dto.AddShoppingCartLineDTO;
import org.delicias.line.service.ShoppingCartLineService;

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

}
