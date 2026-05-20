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


    public Integer getDistance(
            Double addressLng, Double addressLat,
            Double restaurantLng, Double restaurantLat
    ) {

        System.out.println("--------------< Restaurant  LatLng:"+restaurantLat+","+restaurantLng+" >--------------");
        System.out.println("--------------< Destination LatLng:"+addressLat+","+addressLng+" >--------------");

        String sql = """
        SELECT CEIL(ST_Distance(
            ST_SetSRID(ST_MakePoint(:addressLng, :addressLat), 4326)::geography, 
            ST_SetSRID(ST_MakePoint(:restaurantLng, :restaurantLat), 4326)::geography 
        ))
        """;

        Object result = getEntityManager().createNativeQuery(sql)
                .setParameter("addressLng", addressLng)
                .setParameter("addressLat", addressLat)
                .setParameter("restaurantLng", restaurantLng)
                .setParameter("restaurantLat", restaurantLat)
                .getSingleResult();

        System.out.println("--------------< Distancia:"+result+" >--------------");

        return result != null ? ((Number) result).intValue() : null;
    }
}
