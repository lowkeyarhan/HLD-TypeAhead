package com.lowkeyarhan.TypeAhead.modules.trending;

import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import java.util.List;

// Service interface for retrieving trending queries.
public interface TrendingService {
    List<SuggestResultDTO> getTrending(int limit);
}
