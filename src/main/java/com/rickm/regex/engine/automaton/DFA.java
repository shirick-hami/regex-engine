package com.rickm.regex.engine.automaton;

import java.util.*;

/**
 * Deterministic Finite Automaton (DFA) for regex matching.
 * 
 * Built from an NFA using the subset construction algorithm
 * (also known as powerset construction). Each DFA state
 * represents a set of NFA states.
 * 
 * <h2>Properties</h2>
 * <ul>
 *   <li>Exactly one transition per input symbol from each state</li>
 *   <li>No epsilon transitions</li>
 *   <li>Guaranteed O(n) matching time for input of length n</li>
 *   <li>May have exponentially more states than the NFA</li>
 * </ul>
 */
public class DFA {
    
    private final DFAState start;
    private final Set<DFAState> states;
    private final Set<DFAState> acceptingStates;
    
    private DFA(DFAState start, Set<DFAState> states, Set<DFAState> acceptingStates) {
        this.start = start;
        this.states = states;
        this.acceptingStates = acceptingStates;
    }
    
    public DFAState getStart() {
        return start;
    }
    
    public Set<DFAState> getStates() {
        return Collections.unmodifiableSet(states);
    }
    
    public Set<DFAState> getAcceptingStates() {
        return Collections.unmodifiableSet(acceptingStates);
    }
    
    public int getStateCount() {
        return states.size();
    }
    
    /**
     * Builds a DFA from an NFA using subset construction.
     * 
     * Uses lazy construction - only creates states as needed
     * to avoid exponential blowup for most practical patterns.
     */
    public static DFA fromNFA(NFA nfa) {
        DFAState.resetIdCounter();
        
        Set<DFAState> dfaStates = new HashSet<>();
        Set<DFAState> acceptingStates = new HashSet<>();
        Map<Set<NFAState>, DFAState> stateMap = new HashMap<>();
        
        // Start state is epsilon closure of NFA start
        Set<NFAState> startNfaStates = epsilonClosure(Collections.singleton(nfa.getStart()));
        DFAState startState = new DFAState(startNfaStates);
        dfaStates.add(startState);
        stateMap.put(startNfaStates, startState);
        
        if (startState.isAccepting()) {
            acceptingStates.add(startState);
        }
        
        // Worklist algorithm
        Deque<DFAState> worklist = new ArrayDeque<>();
        worklist.add(startState);
        
        // Get all possible input symbols
        Set<Character> alphabet = getAlphabet(startNfaStates);
        
        while (!worklist.isEmpty()) {
            DFAState current = worklist.poll();
            
            // For each possible input symbol
            for (char c : alphabet) {
                Set<NFAState> nextNfaStates = move(current.getNfaStates(), c);
                nextNfaStates = epsilonClosure(nextNfaStates);
                
                if (nextNfaStates.isEmpty()) {
                    continue; // Dead state, no transition needed
                }
                
                DFAState nextState = stateMap.get(nextNfaStates);
                if (nextState == null) {
                    nextState = new DFAState(nextNfaStates);
                    dfaStates.add(nextState);
                    stateMap.put(nextNfaStates, nextState);
                    worklist.add(nextState);
                    
                    if (nextState.isAccepting()) {
                        acceptingStates.add(nextState);
                    }
                    
                    // Expand alphabet with new symbols
                    alphabet.addAll(getAlphabet(nextNfaStates));
                }
                
                current.addTransition(c, nextState);
            }
        }
        
        return new DFA(startState, dfaStates, acceptingStates);
    }
    
    /**
     * Computes epsilon closure of a set of NFA states.
     */
    private static Set<NFAState> epsilonClosure(Set<NFAState> states) {
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
    
    /**
     * Computes states reachable from a set of NFA states on input c.
     */
    private static Set<NFAState> move(Set<NFAState> states, char c) {
        Set<NFAState> result = new HashSet<>();
        for (NFAState state : states) {
            result.addAll(state.getTransitions(c));
        }
        return result;
    }
    
    /**
     * Gets all characters that have transitions from the given states.
     * For simplicity, we use a representative set of printable ASCII.
     */
    private static Set<Character> getAlphabet(Set<NFAState> states) {
        Set<Character> alphabet = new HashSet<>();
        
        // Add common characters - this is a simplification
        // A full implementation would track actual transition symbols
        for (char c = 'a'; c <= 'z'; c++) alphabet.add(c);
        for (char c = 'A'; c <= 'Z'; c++) alphabet.add(c);
        for (char c = '0'; c <= '9'; c++) alphabet.add(c);
        alphabet.add(' ');
        alphabet.add('\t');
        alphabet.add('_');
        alphabet.add('-');
        alphabet.add('.');
        alphabet.add('@');
        alphabet.add(':');
        alphabet.add('/');
        
        return alphabet;
    }
}
