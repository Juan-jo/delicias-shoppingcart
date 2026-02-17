package org.delicias.shoppingcart.domain.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.delicias.shoppingcart.domain.model.ShoppingCart;

import java.util.UUID;

@ApplicationScoped
public class ShoppingCartRepository implements PanacheRepositoryBase<ShoppingCart, UUID> {
}
