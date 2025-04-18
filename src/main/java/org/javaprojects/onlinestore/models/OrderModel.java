package org.javaprojects.onlinestore.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderModel
{
    private Long id;
    private List<ItemModel> items = new ArrayList<>();
    BigDecimal totalSum;

    public OrderModel()
    {
    }

    public OrderModel(Long id, List<ItemModel> items, BigDecimal totalSum)
    {
        this.id = id;
        this.items = items;
        this.totalSum = totalSum;
    }

    public Long getId()
    {
        return id;
    }

    public OrderModel setId(Long id)
    {
        this.id = id;
        return this;
    }

    public List<ItemModel> getItems()
    {
        return items;
    }

    public OrderModel setItems(List<ItemModel> items)
    {
        this.items = items;
        return this;
    }

    public OrderModel addItem(ItemModel itemModel) {
        this.items.add(itemModel);
        return this;
    }

    public BigDecimal getTotalSum()
    {
        return totalSum;
    }

    public OrderModel setTotalSum(BigDecimal totalSum)
    {
        this.totalSum = totalSum;
        return this;
    }
}
