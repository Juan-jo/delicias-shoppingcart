package org.delicias.shoppingcart.domain.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.delicias.shoppingcart.domain.model.ShoppingCart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ShoppingCartRepository implements PanacheRepositoryBase<ShoppingCart, UUID> {

    public List<ShoppingCart> findByUser(UUID userUUID) {
        return list("userUUID", Sort.ascending("id"), userUUID);
    }


    public Optional<ShoppingCart> findByUserAndRestaurant(UUID userUUID, Integer restaurantTmplId) {
        return find("userUUID = ?1 AND restaurantTmplId = ?2", userUUID, restaurantTmplId)
                .firstResultOptional();
    }
}
