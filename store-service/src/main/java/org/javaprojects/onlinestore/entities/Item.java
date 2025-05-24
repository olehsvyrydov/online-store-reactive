package org.javaprojects.onlinestore.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table(name = "items")
public class Item {

    @Id
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String imgPath;

    public Item() {}

    public Item(Long id, String title, String description, BigDecimal price, String imgPath) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.imgPath = imgPath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImgPath() {
        return imgPath;
    }

    public Item setImgPath(String imgPath)
    {
        this.imgPath = imgPath;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item item)) return false;

        if (!id.equals(item.id)) return false;
        if (!title.equals(item.title)) return false;
        if (!description.equals(item.description)) return false;
        if (!price.equals(item.price)) return false;
        return imgPath.equals(item.imgPath);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + price.hashCode();
        result = 31 * result + imgPath.hashCode();
        return result;
    }


}
