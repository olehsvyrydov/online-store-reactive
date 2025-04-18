package org.javaprojects.onlinestore.exceptions;

public class WrongQuantityException extends RuntimeException {
    private Long itemId;
    public WrongQuantityException(String s, Long itemId) {
        super(s);
        this.itemId = itemId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
}
