package org.delicias.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ShoppingBusinessExceptionMapper implements ExceptionMapper<ShoppingBusinessException> {

    @Override
    public Response toResponse(ShoppingBusinessException exception) {
        ErrorResponse error = new ErrorResponse(
                exception.getMessage(),
                exception.getErrorCode()
        );

        return Response.status(exception.getStatus())
                .entity(error)
                .build();
    }
}
