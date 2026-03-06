package org.delicias.shoppingcart.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.delicias.common.adjusment.OrderAdjustment;
import org.delicias.line.domain.model.ShoppingCartLine;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shopping_cart")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_uuid")
    private UUID userUUID;

    @Column(name = "restaurant_tmpl_id")
    private Integer restaurantTmplId;

    @Column(name = "user_address_id")
    private Integer userAddressId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<OrderAdjustment> adjustments;


    @Formula("(SELECT COUNT(*) FROM shopping_cart_line s WHERE s.shopping_cart_uuid = id)")
    private Integer lineCount;

    @OrderBy("id asc")
    @OneToMany(mappedBy = "shoppingCart",  cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ShoppingCartLine> lines;

    public void addAdjustment(OrderAdjustment adjustment) {
        if(this.adjustments == null) {
            this.adjustments = new ArrayList<>();
        }

        this.adjustments.add(adjustment);
    }
}
