package org.javaprojects.onlinestore.models;

public record Paging
(
    int pageNumber,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious
){}
