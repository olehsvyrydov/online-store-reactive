package org.javaprojects.onlinestore.models;


import org.springframework.data.domain.PageImpl;

import java.util.List;

public class AllItemsModel {
    List<ItemModel> productList;
    Paging paging;

    public AllItemsModel(List<ItemModel> productList, PageImpl<ItemModel> page) {
        this.productList = productList;
        this.paging = new Paging(
                page.getNumber(),
                page.getSize(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    public List<ItemModel> getProductList() {
        return productList;
    }

    public Paging getPaging() {
        return paging;
    }
}
