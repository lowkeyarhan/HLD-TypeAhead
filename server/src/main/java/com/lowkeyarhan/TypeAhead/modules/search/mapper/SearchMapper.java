package com.lowkeyarhan.TypeAhead.modules.search.mapper;

import com.lowkeyarhan.TypeAhead.modules.search.dto.SearchRequestDTO;

public class SearchMapper {
    public static String toQueryText(SearchRequestDTO request) {
        if (request == null) {
            return null;
        }
        return request.query();
    }
}
