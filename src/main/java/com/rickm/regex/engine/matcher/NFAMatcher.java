package com.rickm.regex.engine.matcher;

import com.rickm.regex.engine.automaton.NFA;
import com.rickm.regex.engine.automaton.NFAState;
import com.rickm.regex.engine.dto.MatchResult;
import com.rickm.regex.engine.exception.RegexTimeoutException;
import com.rickm.regex.engine.parser.AstNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * NFA-based regex matcher using Thompson's simulation algorithm.
 *
 * This matcher simulates the NFA by tracking all possible states
 * simultaneously, avoiding the exponential time complexity of
 * backtracking for pathological cases.
 *
 * <h2>Algorithm Overview</h2>
 * <ol>
 *   <li>Start with the epsilon closure of the start state</li>
 *   <li>For each input character, compute the next set of states</li>
 *   <li>Take the epsilon closure of the new states</li>
 *   <li>If any state in the final set is accepting, we have a match</li>
 * </ol>
 *
 * <h2>Time Complexity</h2>
 * O(n * m) where n is input length and m is number of NFA states.
 * This is linear in the input size for a fixed pattern.
 */
@Slf4j
public class NFAMatcher implements RegexMatcher {

    private final NFA nfa;
    private final long timeoutMs;
    private long startTime;
    private long stateTransitions;

    /**
     * Creates a new NFA matcher.
     *
     * @param ast the regex AST
     * @param timeoutMs maximum execution time
     */
    public NFAMatcher(AstNode ast, long timeoutMs) {
        this.nfa = NFA.fromAST(ast);
        this.timeoutMs = timeoutMs;
    }

    /**
     * Attempts to match the pattern against the entire input string.
     */
    @Override
    public MatchResult matchFull(String input) {
        this.startTime = System.currentTimeMillis();
        this.stateTransitions = 0;

        log.debug("NFA full match against input of length {}", input.length());

        Set<NFAState> currentStates = epsilonClosure(Collections.singleton(nfa.getStart()));

        for (int i = 0; i < input.length(); i++) {
            checkTimeout();
            char c = input.charAt(i);
            currentStates = move(currentStates, c);
            currentStates = epsilonClosure(currentStates);

            if (currentStates.isEmpty()) {
                return buildResult(input, false, 0, 0);
            }
        }

        boolean matched = currentStates.stream().anyMatch(NFAState::isAccepting);
        return buildResult(input, matched, 0, matched ? input.length() : 0);
    }

    /**
     * Finds the first match in the input string.
     */
    @Override
    public MatchResult find(String input) {
        this.startTime = System.currentTimeMillis();
        this.stateTransitions = 0;

        log.debug("NFA find in input of length {}", input.length());

        for (int startPos = 0; startPos <= input.length(); startPos++) {
            checkTimeout();

            Set<NFAState> currentStates = epsilonClosure(Collections.singleton(nfa.getStart()));

            // Check for zero-width match at start
            if (currentStates.stream().anyMatch(NFAState::isAccepting)) {
                return buildResult(input, true, startPos, startPos);
            }

            for (int i = startPos; i < input.length(); i++) {
                checkTimeout();
                char c = input.charAt(i);
                currentStates = move(currentStates, c);
                currentStates = epsilonClosure(currentStates);

                if (currentStates.isEmpty()) {
                    break;
                }

                if (currentStates.stream().anyMatch(NFAState::isAccepting)) {
                    return buildResult(input, true, startPos, i + 1);
                }
            }
        }

        return buildResult(input, false, -1, -1);
    }

    /**
     * Finds all non-overlapping matches in the input string.
     */
    @Override
    public MatchResult findAll(String input) {
        this.startTime = System.currentTimeMillis();
        this.stateTransitions = 0;

        log.debug("NFA findAll in input of length {}", input.length());

        List<MatchResult.MatchInfo> allMatches = new ArrayList<>();
        int searchStart = 0;

        while (searchStart <= input.length()) {
            checkTimeout();

            int matchStart = -1;
            int matchEnd = -1;

            for (int startPos = searchStart; startPos <= input.length(); startPos++) {
                Set<NFAState> currentStates = epsilonClosure(Collections.singleton(nfa.getStart()));

                // Check for zero-width match
                if (currentStates.stream().anyMatch(NFAState::isAccepting)) {
                    matchStart = startPos;
                    matchEnd = startPos;
                }

                for (int i = startPos; i < input.length(); i++) {
                    checkTimeout();
                    char c = input.charAt(i);
                    currentStates = move(currentStates, c);
                    currentStates = epsilonClosure(currentStates);

                    if (currentStates.isEmpty()) {
                        break;
                    }

                    if (currentStates.stream().anyMatch(NFAState::isAccepting)) {
                        matchStart = startPos;
                        matchEnd = i + 1;
                        // Continue to find longest match (greedy)
                    }
                }

                if (matchStart >= 0) {
                    break;
                }
            }

            if (matchStart >= 0) {
                allMatches.add(MatchResult.MatchInfo.builder()
                        .startIndex(matchStart)
                        .endIndex(matchEnd)
                        .matchedText(input.substring(matchStart, matchEnd))
                        .build());

                searchStart = matchEnd > matchStart ? matchEnd : matchStart + 1;
            } else {
                break;
            }
        }

        long elapsedMs = System.currentTimeMillis() - startTime;

        MatchResult result = MatchResult.builder()
                .matched(!allMatches.isEmpty())
                .input(input)
                .pattern("")
                .allMatches(allMatches)
                .backtrackCount(stateTransitions)
                .matchTimeMs(elapsedMs)
                .build();

        if (!allMatches.isEmpty()) {
            result.setStartIndex(allMatches.get(0).getStartIndex());
            result.setEndIndex(allMatches.get(0).getEndIndex());
            result.setMatchedText(allMatches.get(0).getMatchedText());
        }

        return result;
    }

    /**
     * Computes epsilon closure of a set of states.
     */
    private Set<NFAState> epsilonClosure(Set<NFAState> states) {
        Set<NFAState> closure = new HashSet<>();
        Deque<NFAState> stack = new ArrayDeque<>(states);

        while (!stack.isEmpty()) {
            NFAState state = stack.pop();
            if (closure.add(state)) {
                stateTransitions++;
                for (NFAState next : state.getEpsilonTransitions()) {
                    if (!closure.contains(next)) {
                        stack.push(next);
                    }
                }
            }
        }

        return closure;
    }

    /**
     * Computes the set of states reachable from current states on input character.
     */
    private Set<NFAState> move(Set<NFAState> states, char c) {
        Set<NFAState> result = new HashSet<>();
        for (NFAState state : states) {
            stateTransitions++;
            result.addAll(state.getTransitions(c));
        }
        return result;
    }

    private void checkTimeout() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > timeoutMs) {
            throw new RegexTimeoutException("", "", timeoutMs, elapsed);
        }
    }

    private MatchResult buildResult(String input, boolean matched, int start, int end) {
        long elapsedMs = System.currentTimeMillis() - startTime;

        MatchResult result;
        if (matched) {
            result = MatchResult.match(input, "", start, end);
        } else {
            result = MatchResult.noMatch(input, "");
        }
        result.setBacktrackCount(stateTransitions);
        result.setMatchTimeMs(elapsedMs);
        return result;
    }
}
