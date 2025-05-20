package org.javaprojects.onlinestore.models;

import java.util.List;

public class AllItemsModel {
    List<ItemModel> productList;
    Paging paging;

    public AllItemsModel(List<ItemModel> productList, Paging paging) {
        this.productList = productList;
        this.paging = paging;
    }

    public List<ItemModel> getProductList() {
        return productList;
    }

    public Paging getPaging() {
        return paging;
    }
}
