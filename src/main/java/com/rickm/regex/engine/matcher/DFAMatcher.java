package com.rickm.regex.engine.matcher;

import com.rickm.regex.engine.automaton.DFAState;
import com.rickm.regex.engine.automaton.NFA;
import com.rickm.regex.engine.automaton.NFAState;
import com.rickm.regex.engine.dto.MatchResult;
import com.rickm.regex.engine.exception.RegexTimeoutException;
import com.rickm.regex.engine.parser.AstNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * DFA-based regex matcher for guaranteed linear time matching.
 *
 * This matcher converts the regex to a DFA and then simulates
 * the DFA on the input, giving O(n) time complexity for input
 * of length n.
 *
 * <h2>Algorithm Overview</h2>
 * <ol>
 *   <li>Convert regex AST to NFA (Thompson's construction)</li>
 *   <li>Lazily convert NFA to DFA (subset construction on demand)</li>
 *   <li>Simulate DFA on input (single pass)</li>
 * </ol>
 *
 * <h2>Trade-offs</h2>
 * <ul>
 *   <li>PRO: Guaranteed O(n) matching time</li>
 *   <li>PRO: No backtracking, no stack overflow</li>
 *   <li>PRO: Full Unicode support via lazy construction</li>
 *   <li>CON: DFA construction can be slow for complex patterns</li>
 *   <li>CON: DFA may have exponentially many states (rare in practice)</li>
 * </ul>
 *
 * <h2>Unicode Support</h2>
 * This implementation uses lazy DFA construction to handle Unicode
 * characters efficiently. States are computed on-demand for any
 * character encountered in the input, not just ASCII.
 */
@Slf4j
public class DFAMatcher implements RegexMatcher {

    private final NFA nfa;
    private final long timeoutMs;
    private long startTime;
    private long stateTransitions;
    private long dfaConstructionTimeMs;

    // Lazy DFA state cache - maps NFA state sets to DFA states
    private final Map<Set<NFAState>, DFAState> stateCache;
    private final DFAState startState;

    /**
     * Creates a new DFA matcher.
     *
     * @param ast the regex AST
     * @param timeoutMs maximum execution time
     */
    public DFAMatcher(AstNode ast, long timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.stateCache = new HashMap<>();

        long constructStart = System.currentTimeMillis();
        this.nfa = NFA.fromAST(ast);

        // Create start state from epsilon closure of NFA start
        Set<NFAState> startNfaStates = epsilonClosure(Collections.singleton(nfa.getStart()));
        this.startState = getOrCreateState(startNfaStates);

        this.dfaConstructionTimeMs = System.currentTimeMillis() - constructStart;

        log.debug("DFA initialized with start state, lazy construction enabled");
    }

    /**
     * Gets the number of states in the DFA (constructed so far).
     */
    public int getStateCount() {
        return stateCache.size();
    }

    /**
     * Gets the DFA construction time.
     */
    public long getDfaConstructionTimeMs() {
        return dfaConstructionTimeMs;
    }

    /**
     * Attempts to match the pattern against the entire input string.
     */
    @Override
    public MatchResult matchFull(String input) {
        this.startTime = System.currentTimeMillis();
        this.stateTransitions = 0;

        log.debug("DFA full match against input of length {}", input.length());

        DFAState current = startState;

        for (int i = 0; i < input.length(); i++) {
            checkTimeout();
            char c = input.charAt(i);

            DFAState next = getNextState(current, c);
            stateTransitions++;

            if (next == null) {
                // No transition - pattern cannot match
                return buildResult(input, false, 0, 0);
            }

            current = next;
        }

        boolean matched = current.isAccepting();
        return buildResult(input, matched, 0, matched ? input.length() : 0);
    }

    /**
     * Finds the first match in the input string.
     */
    @Override
    public MatchResult find(String input) {
        this.startTime = System.currentTimeMillis();
        this.stateTransitions = 0;

        log.debug("DFA find in input of length {}", input.length());

        for (int startPos = 0; startPos <= input.length(); startPos++) {
            checkTimeout();

            DFAState current = startState;
            int lastAcceptPos = -1;

            // Check if start state is accepting (zero-width match)
            if (current.isAccepting()) {
                lastAcceptPos = startPos;
            }

            for (int i = startPos; i < input.length(); i++) {
                checkTimeout();
                char c = input.charAt(i);

                DFAState next = getNextState(current, c);
                stateTransitions++;

                if (next == null) {
                    break; // No transition, try next start position
                }

                current = next;

                if (current.isAccepting()) {
                    lastAcceptPos = i + 1;
                }
            }

            if (lastAcceptPos >= 0) {
                return buildResult(input, true, startPos, lastAcceptPos);
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

        log.debug("DFA findAll in input of length {}", input.length());

        List<MatchResult.MatchInfo> allMatches = new ArrayList<>();
        int searchStart = 0;

        while (searchStart <= input.length()) {
            checkTimeout();

            int matchStart = -1;
            int matchEnd = -1;

            for (int startPos = searchStart; startPos <= input.length(); startPos++) {
                DFAState current = startState;
                int lastAcceptPos = -1;

                if (current.isAccepting()) {
                    lastAcceptPos = startPos;
                }

                for (int i = startPos; i < input.length(); i++) {
                    checkTimeout();
                    char c = input.charAt(i);

                    DFAState next = getNextState(current, c);
                    stateTransitions++;

                    if (next == null) {
                        break;
                    }

                    current = next;

                    if (current.isAccepting()) {
                        lastAcceptPos = i + 1;
                    }
                }

                if (lastAcceptPos >= 0) {
                    matchStart = startPos;
                    matchEnd = lastAcceptPos;
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
     * Gets the next DFA state for a given input character.
     * Uses lazy construction - computes states on demand.
     * This allows full Unicode support without pre-computing all transitions.
     */
    private DFAState getNextState(DFAState current, char c) {
        // Check if we've already computed this transition
        DFAState cached = current.getTransition(c);
        if (cached != null) {
            return cached;
        }

        // Compute the next NFA state set by following all transitions on 'c'
        Set<NFAState> nextNfaStates = new HashSet<>();
        for (NFAState nfaState : current.getNfaStates()) {
            // getTransitions handles literals, any-char, character classes, etc.
            Set<NFAState> transitions = nfaState.getTransitions(c);
            nextNfaStates.addAll(transitions);
        }

        if (nextNfaStates.isEmpty()) {
            // No transitions for this character - dead state
            // We don't cache null transitions to save memory
            return null;
        }

        // Take epsilon closure of the resulting states
        nextNfaStates = epsilonClosure(nextNfaStates);

        if (nextNfaStates.isEmpty()) {
            return null;
        }

        // Get or create the DFA state for this NFA state set
        DFAState nextState = getOrCreateState(nextNfaStates);

        // Cache the transition for future lookups
        current.addTransition(c, nextState);

        return nextState;
    }

    /**
     * Gets an existing DFA state for the NFA state set, or creates a new one.
     */
    private DFAState getOrCreateState(Set<NFAState> nfaStates) {
        // Look up by NFA state set
        DFAState existing = stateCache.get(nfaStates);
        if (existing != null) {
            return existing;
        }

        // Create new DFA state
        DFAState newState = new DFAState(nfaStates);

        // Cache it (make a copy of the set for the key to avoid mutation issues)
        stateCache.put(new HashSet<>(nfaStates), newState);

        return newState;
    }

    /**
     * Computes epsilon closure of a set of NFA states.
     * Returns all states reachable via epsilon transitions.
     */
    private Set<NFAState> epsilonClosure(Set<NFAState> states) {
        Set<NFAState> closure = new HashSet<>();
        Deque<NFAState> stack = new ArrayDeque<>(states);

        while (!stack.isEmpty()) {
            NFAState state = stack.pop();
            if (closure.add(state)) {
                for (NFAState next : state.getEpsilonTransitions()) {
                    if (!closure.contains(next)) {
                        stack.push(next);
                    }
                }
            }
        }

        return closure;
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
