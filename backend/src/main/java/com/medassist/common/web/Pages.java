package com.medassist.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Clamps client-supplied paging so nobody can ask for 10 million rows. */
public final class Pages {

    public static final int MAX_SIZE = 100;

    private Pages() {
    }

    public static Pageable of(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_SIZE));
    }

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_SIZE), sort);
    }
}
