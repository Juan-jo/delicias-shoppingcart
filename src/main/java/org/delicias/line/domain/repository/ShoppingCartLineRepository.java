package org.delicias.line.domain.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.delicias.line.domain.model.ShoppingCartLine;

import java.util.UUID;

@ApplicationScoped
public class ShoppingCartLineRepository implements PanacheRepositoryBase<ShoppingCartLine, UUID> {
}
