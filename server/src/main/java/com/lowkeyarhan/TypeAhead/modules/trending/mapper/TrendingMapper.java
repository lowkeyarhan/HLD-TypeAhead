package com.lowkeyarhan.TypeAhead.modules.trending.mapper;

import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import com.lowkeyarhan.TypeAhead.modules.trending.dto.TrendingResponseDTO;
import java.util.List;

public class TrendingMapper {
    public static TrendingResponseDTO toResponseDTO(List<SuggestResultDTO> trendingList) {
        return new TrendingResponseDTO(trendingList);
    }
}
