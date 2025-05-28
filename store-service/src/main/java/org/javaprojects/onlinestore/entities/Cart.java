package org.javaprojects.onlinestore.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "cart")
public class Cart {

    @Id
    private Long itemId;
    private Long userId;
    private long quantity;

    public Cart(Long itemId, Long userId, long quantity)
    {
        this.itemId = itemId;
        this.userId = userId;
        this.quantity = quantity;
    }

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public Cart setUserId(Long userId)
    {
        this.userId = userId;
        return this;
    }

    public long getQuantity()
    {
        return quantity;
    }

    public Cart setQuantity(long quantity)
    {
        this.quantity = quantity;
        return this;
    }
}

