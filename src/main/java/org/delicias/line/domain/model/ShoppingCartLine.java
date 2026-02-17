package org.delicias.line.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.delicias.shoppingcart.domain.model.ShoppingCart;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shopping_cart_line")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCartLine {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "shopping_cart_uuid", referencedColumnName = "id")
    private ShoppingCart shoppingCart;

    @Column(name = "product_tmpl_id")
    private Integer productTmplId;

    @Column(name = "qty")
    private Short qty;

    @Column(columnDefinition = "int[]", name = "attr_value_ids")
    private Set<Integer> attrValuesIds;

    public void updateQty(Short qty, Set<Integer> attrValuesIds) {
        this.qty = qty;
        this.attrValuesIds = attrValuesIds;
    }
}
