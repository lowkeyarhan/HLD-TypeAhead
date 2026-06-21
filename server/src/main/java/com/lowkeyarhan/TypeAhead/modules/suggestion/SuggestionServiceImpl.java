package com.lowkeyarhan.TypeAhead.modules.suggestion;

import com.lowkeyarhan.TypeAhead.modules.index.PrefixIndexService;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// Implementation class for SuggestionService, retrieving results from prefix index.
@Service
public class SuggestionServiceImpl implements SuggestionService {

    private final PrefixIndexService prefixIndexService;

    public SuggestionServiceImpl(PrefixIndexService prefixIndexService) {
        this.prefixIndexService = prefixIndexService;
    }

    @Override
    public List<SuggestResultDTO> getSuggestions(String prefix, int limit) {
        return prefixIndexService.search(prefix, limit).stream()
                .map(qc -> new SuggestResultDTO(qc.getQueryText(), qc.getTotalCount()))
                .collect(Collectors.toList());
    }
}
