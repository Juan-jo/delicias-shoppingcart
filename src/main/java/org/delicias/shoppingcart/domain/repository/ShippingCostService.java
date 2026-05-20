package org.delicias.shoppingcart.domain.repository;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShippingCostService {

    public double calculate(Integer distance) {

        final double minimumShippingCost = 30.0;

        final int baseDistance = 2000; // 2 km en metros
        final int extraKmMeters = 1000; // 1 km
        final double extraCostPerKm = 5.0;

        if (distance == null || distance <= baseDistance) {
            return minimumShippingCost;
        }

        int extraDistance = distance - baseDistance;

        int extraKm = (int) Math.ceil(extraDistance / (double) extraKmMeters);

        return minimumShippingCost + (extraKm * extraCostPerKm);
    }
}
