package com.hibuka.soda.foundation.io;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Page View Object, used for controller layer response to frontend.
 * Inherits from PageDTO and adds additional pagination metadata for frontend display.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PageVO<T> extends PageDTO<T> {
    private static final long serialVersionUID = 1L;

    /**
     * Current page number, starting from 1
     */
    private Integer pageNo = 1;

    /**
     * Page size
     */
    private Integer pageSize = 10;

    /**
     * Total number of pages
     */
    private Long pages = 0L;

    /**
     * Whether there is a next page
     */
    private Boolean hasNext = false;

    /**
     * Whether there is a previous page
     */
    private Boolean hasPrevious = false;

    /**
     * Current page start record index
     */
    private Long startIndex = 0L;

    /**
     * Current page end record index
     */
    private Long endIndex = 0L;

    /**
     * Default constructor
     */
    public PageVO() {
        super();
    }

    /**
     * Constructor with total, list, pageNo, and pageSize
     * @param total total count of records
     * @param list list of data records
     * @param pageNo current page number
     * @param pageSize page size
     */
    public PageVO(Long total, List<T> list, Integer pageNo, Integer pageSize) {
        super(total, list);
        this.pageNo = pageNo != null ? Math.max(pageNo, 1) : 1;
        this.pageSize = pageSize != null ? Math.min(Math.max(pageSize, 1), 100) : 10;
        calculatePaginationMetadata();
    }

    /**
     * Constructor with PageDTO, pageNo, and pageSize
     * @param pageDTO page data transfer object
     * @param pageNo current page number
     * @param pageSize page size
     */
    public PageVO(PageDTO<T> pageDTO, Integer pageNo, Integer pageSize) {
        super(pageDTO.getTotal(), pageDTO.getList());
        this.pageNo = pageNo != null ? Math.max(pageNo, 1) : 1;
        this.pageSize = pageSize != null ? Math.min(Math.max(pageSize, 1), 100) : 10;
        calculatePaginationMetadata();
    }

    /**
     * Constructor with PageDTO and BasePageRequest
     * @param pageDTO page data transfer object
     * @param pageRequest page request object
     */
    public PageVO(PageDTO<T> pageDTO, BasePageRequest pageRequest) {
        super(pageDTO.getTotal(), pageDTO.getList());
        this.pageNo = pageRequest.getPageNo();
        this.pageSize = pageRequest.getPageSize();
        calculatePaginationMetadata();
    }

    /**
     * Calculates pagination metadata based on total, pageNo, and pageSize
     */
    private void calculatePaginationMetadata() {
        // Calculate total pages
        Long total = getTotal() != null ? getTotal() : 0L;
        this.pages = total == 0 ? 0L : (total - 1) / this.pageSize + 1;
        
        // Calculate hasNext and hasPrevious
        this.hasNext = this.pageNo < this.pages;
        this.hasPrevious = this.pageNo > 1;
        
        // Calculate start and end indexes
        this.startIndex = (long) (this.pageNo - 1) * this.pageSize + 1;
        this.endIndex = Math.min(this.startIndex + this.pageSize - 1, total);
        if (total == 0) {
            this.startIndex = 0L;
            this.endIndex = 0L;
        }
    }
}