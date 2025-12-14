package com.hibuka.soda.foundation.io;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base page request class, extending BaseRequest, providing page number and page size for paging requests.
 * This class inherits all fields from BaseRequest and adds pagination-specific functionality.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class BasePageRequest extends BaseRequest {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "pageNo cannot be null")
    private Integer pageNo = 1;
    @NotNull(message = "pageSize cannot be null")
    private Integer pageSize = 10;

    private Integer offset;
    private Integer limit;

    /**
     * Sets the page number, ensuring it's at least 1.
     * @param pageNo page number
     */
    public void setPageNo(Integer pageNo) {
        this.pageNo = Math.max(pageNo, 1);
        this.offset = (this.pageNo - 1) * this.pageSize;
        this.limit = this.pageSize;
    }

    /**
     * Sets the page size, ensuring it's between 1 and 100.
     * @param pageSize page size
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = Math.min(Math.max(pageSize, 1), 100);
        this.offset = (this.pageNo - 1) * this.pageSize;
        this.limit = this.pageSize;
    }

    /**
     * Gets the offset, calculated from pageNo and pageSize if not explicitly set.
     * @return offset
     */
    public Integer getOffset() {
        if (offset == null) {
            return (getPageNo() - 1) * getPageSize();
        }
        return offset;
    }

    /**
     * Gets the limit, same as pageSize if not explicitly set.
     * @return limit
     */
    public Integer getLimit() {
        if (limit == null) {
            return getPageSize();
        }
        return limit;
    }

    /**
     * Gets the page number with default value 1 if null.
     * @return page number
     */
    public Integer getPageNumberWithDefault() {
        return this.pageNo != null ? this.pageNo : 1;
    }

    /**
     * Gets the page size with default value 10 if null.
     * @return page size
     */
    public Integer getPageSizeWithDefault() {
        return this.pageSize != null ? this.pageSize : 10;
    }
}