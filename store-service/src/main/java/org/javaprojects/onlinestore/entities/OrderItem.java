package org.javaprojects.onlinestore.entities;

import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_items")
public class OrderItem {
    private Long orderId;
    private Long itemId;

    @Transient
    private Item item;
    @Transient
    private Order order;

    private long quantity;

    public Long getOrderId()
    {
        return orderId;
    }

    public OrderItem setOrderId(Long orderId)
    {
        this.orderId = orderId;
        return this;
    }

    public Long getItemId()
    {
        return itemId;
    }

    public OrderItem setItemId(Long itemId)
    {
        this.itemId = itemId;
        return this;
    }

    public Item getItem()
    {
        return item;
    }

    public OrderItem setItem(Item item)
    {
        this.item = item;
        return this;
    }

    public Order getOrder()
    {
        return order;
    }

    public OrderItem setOrder(Order order)
    {
        this.order = order;
        return this;
    }

    public long getQuantity()
    {
        return quantity;
    }

    public OrderItem setQuantity(long quantity)
    {
        this.quantity = quantity;
        return this;
    }
}
