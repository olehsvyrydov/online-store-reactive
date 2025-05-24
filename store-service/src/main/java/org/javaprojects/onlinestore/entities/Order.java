package org.javaprojects.onlinestore.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;

@Table("orders")
public class Order {
    @Id
    private Long id;

    private BigDecimal total;

    @Column("user_id")
    private Long userId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Long getUserId()
    {
        return userId;
    }

    public Order setUserId(Long userId)
    {
        this.userId = userId;
        return this;
    }
}
