package org.delicias.shoppingcart.domain.repository;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShippingCostService {

    public double calculate(Integer distance) {


        double minimumShippingCost = 30.0;

        long _1km = 1000L; // 1 km = 1000 meters
        Long _2km = 2000L; // 2 km = 2000 meters

        if(distance > _2km) {

            long km = _2km;

            while (km < distance) {

                minimumShippingCost += 5d;
                km += _1km;
            }
        }

        return minimumShippingCost;

    }
}
