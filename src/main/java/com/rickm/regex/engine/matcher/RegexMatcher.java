package com.rickm.regex.engine.matcher;


import com.rickm.regex.engine.dto.MatchResult;

public interface RegexMatcher {
    MatchResult matchFull(String inputStr);
    MatchResult find(String inputStr);
    MatchResult findAll(String inputStr);
}
