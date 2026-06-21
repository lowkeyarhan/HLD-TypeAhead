package com.lowkeyarhan.TypeAhead.modules.suggestion.mapper;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import java.util.List;
import java.util.stream.Collectors;

public class SuggestionMapper {
    public static SuggestResultDTO toSuggestResultDTO(QueryCount queryCount) {
        if (queryCount == null) {
            return null;
        }
        return new SuggestResultDTO(queryCount.getQueryText(), queryCount.getTotalCount());
    }

    public static List<SuggestResultDTO> toSuggestResultDTOList(List<QueryCount> queryCounts) {
        if (queryCounts == null) {
            return List.of();
        }
        return queryCounts.stream()
                .map(SuggestionMapper::toSuggestResultDTO)
                .collect(Collectors.toList());
    }
}
