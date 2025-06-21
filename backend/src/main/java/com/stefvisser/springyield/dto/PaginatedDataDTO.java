package com.stefvisser.springyield.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for paginated data responses.
 * <p>
 * This generic class encapsulates paginated data of any type, along with metadata
 * about the total number of items available. It is designed to standardize
 * the response format for API endpoints that implement pagination.
 * </p>
 *
 * @param <T> The type of data being paginated
 */
@Data
@AllArgsConstructor
public class PaginatedDataDTO<T> {

    /**
     * The paginated data content.
     * <p>
     * This field contains the actual data for the current page.
     * The type depends on the generic type parameter used when instantiating the class.
     * </p>
     */
    List<T> data;

    /**
     * The total number of items available across all pages.
     * <p>
     * This count represents the total number of items that match the query criteria,
     * not just the items in the current page. It can be used by clients to determine
     * the total number of pages and implement pagination controls.
     * </p>
     */
    int totalCount;

}
