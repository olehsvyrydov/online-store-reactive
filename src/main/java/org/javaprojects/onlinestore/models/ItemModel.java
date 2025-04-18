package org.javaprojects.onlinestore.models;


import org.javaprojects.onlinestore.entities.Item;

import java.math.BigDecimal;

public class ItemModel {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String imgPath;
    private int count;

    public ItemModel() {
    }

    public ItemModel(Item item, int count) {
        this.id = item.getId();
        this.title = item.getTitle();
        this.description = item.getDescription();
        this.price = item.getPrice();
        this.imgPath = item.getImgPath();
        this.count = count;
    }

    public ItemModel(Long id, String title, String description, BigDecimal price, String imgPath, int count) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.imgPath = imgPath;
        this.count = count;
    }

    public Long getId()
    {
        return id;
    }

    public ItemModel setId(Long id)
    {
        this.id = id;
        return this;
    }

    public String getTitle()
    {
        return title;
    }

    public ItemModel setTitle(String title)
    {
        this.title = title;
        return this;
    }

    public String getDescription()
    {
        return description;
    }

    public ItemModel setDescription(String description)
    {
        this.description = description;
        return this;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public ItemModel setPrice(BigDecimal price)
    {
        this.price = price;
        return this;
    }

    public String getImgPath()
    {
        return imgPath;
    }

    public ItemModel setImgPath(String imgPath)
    {
        this.imgPath = imgPath;
        return this;
    }

    public int getCount()
    {
        return count;
    }

    public ItemModel setCount(int count)
    {
        this.count = count;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemModel itemModel)) return false;

        if (count != itemModel.count) return false;
        if (!id.equals(itemModel.id)) return false;
        if (!title.equals(itemModel.title)) return false;
        if (!description.equals(itemModel.description)) return false;
        if (!price.equals(itemModel.price)) return false;
        return imgPath.equals(itemModel.imgPath);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + price.hashCode();
        result = 31 * result + imgPath.hashCode();
        result = 31 * result + count;
        return result;
    }
}
