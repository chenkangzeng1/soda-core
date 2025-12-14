package com.hibuka.soda.foundation.io;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Page Data Transfer Object, used for service layer internal data transfer.
 * This is a simple container for paginated data, containing only core pagination information.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
@Data
public class PageDTO<T> {
    private static final long serialVersionUID = 1L;

    /** Total count of records */
    private Long total = 0L;

    /** List of data records */
    private List<T> list = Collections.emptyList();

    /**
     * Default constructor with empty list and zero total.
     */
    public PageDTO() {
    }

    /**
     * Constructor with total and list.
     * @param total total count of records
     * @param list list of data records
     */
    public PageDTO(Long total, List<T> list) {
        this.total = total != null ? total : 0L;
        this.list = list != null ? list : Collections.emptyList();
    }
}