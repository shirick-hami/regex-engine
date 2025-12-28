package com.rickm.regex.engine.automaton;

import java.util.*;

/**
 * Represents a state in a Non-deterministic Finite Automaton (NFA).
 * 
 * An NFA state can have multiple transitions on the same symbol,
 * and can have epsilon (ε) transitions that don't consume input.
 */
public class NFAState {
    
    private static int nextId = 0;
    
    private final int id;
    private boolean accepting;
    
    /** Transitions on specific characters */
    private final Map<Character, Set<NFAState>> transitions;
    
    /** Epsilon transitions (no input consumed) */
    private final Set<NFAState> epsilonTransitions;
    
    /** Special transition for "any character" (dot) */
    private Set<NFAState> anyCharTransitions;
    
    /** Transitions for character classes */
    private final Map<Set<Character>, Set<NFAState>> charClassTransitions;
    
    /** Transitions for negated character classes */
    private final Map<Set<Character>, Set<NFAState>> negatedCharClassTransitions;
    
    /** Whitespace transitions */
    private Set<NFAState> whitespaceTransitions;
    
    public NFAState() {
        this.id = nextId++;
        this.accepting = false;
        this.transitions = new HashMap<>();
        this.epsilonTransitions = new HashSet<>();
        this.anyCharTransitions = new HashSet<>();
        this.charClassTransitions = new HashMap<>();
        this.negatedCharClassTransitions = new HashMap<>();
        this.whitespaceTransitions = new HashSet<>();
    }
    
    public static void resetIdCounter() {
        nextId = 0;
    }
    
    public int getId() {
        return id;
    }
    
    public boolean isAccepting() {
        return accepting;
    }
    
    public void setAccepting(boolean accepting) {
        this.accepting = accepting;
    }
    
    /**
     * Adds a transition on a specific character.
     */
    public void addTransition(char c, NFAState target) {
        transitions.computeIfAbsent(c, k -> new HashSet<>()).add(target);
    }
    
    /**
     * Adds an epsilon transition.
     */
    public void addEpsilonTransition(NFAState target) {
        epsilonTransitions.add(target);
    }
    
    /**
     * Adds a transition for any character (dot).
     */
    public void addAnyCharTransition(NFAState target) {
        anyCharTransitions.add(target);
    }
    
    /**
     * Adds a transition for a character class.
     */
    public void addCharClassTransition(Set<Character> chars, NFAState target) {
        charClassTransitions.computeIfAbsent(chars, k -> new HashSet<>()).add(target);
    }
    
    /**
     * Adds a transition for a negated character class.
     */
    public void addNegatedCharClassTransition(Set<Character> chars, NFAState target) {
        negatedCharClassTransitions.computeIfAbsent(chars, k -> new HashSet<>()).add(target);
    }
    
    /**
     * Adds a whitespace transition.
     */
    public void addWhitespaceTransition(NFAState target) {
        whitespaceTransitions.add(target);
    }
    
    /**
     * Gets all states reachable on a specific character.
     */
    public Set<NFAState> getTransitions(char c) {
        Set<NFAState> result = new HashSet<>();
        
        // Direct character transitions
        Set<NFAState> direct = transitions.get(c);
        if (direct != null) {
            result.addAll(direct);
        }
        
        // Any character transitions (except newlines)
        if (c != '\n' && c != '\r') {
            result.addAll(anyCharTransitions);
        }
        
        // Character class transitions
        for (Map.Entry<Set<Character>, Set<NFAState>> entry : charClassTransitions.entrySet()) {
            if (entry.getKey().contains(c)) {
                result.addAll(entry.getValue());
            }
        }
        
        // Negated character class transitions
        for (Map.Entry<Set<Character>, Set<NFAState>> entry : negatedCharClassTransitions.entrySet()) {
            if (!entry.getKey().contains(c) && c != '\n' && c != '\r') {
                result.addAll(entry.getValue());
            }
        }
        
        // Whitespace transitions
        if (Character.isWhitespace(c)) {
            result.addAll(whitespaceTransitions);
        }
        
        return result;
    }
    
    /**
     * Gets all epsilon transitions.
     */
    public Set<NFAState> getEpsilonTransitions() {
        return Collections.unmodifiableSet(epsilonTransitions);
    }
    
    /**
     * Computes the epsilon closure of this state.
     * Returns all states reachable via epsilon transitions.
     */
    public Set<NFAState> epsilonClosure() {
        Set<NFAState> closure = new HashSet<>();
        Deque<NFAState> stack = new ArrayDeque<>();
        
        closure.add(this);
        stack.push(this);
        
        while (!stack.isEmpty()) {
            NFAState state = stack.pop();
            for (NFAState next : state.epsilonTransitions) {
                if (!closure.contains(next)) {
                    closure.add(next);
                    stack.push(next);
                }
            }
        }
        
        return closure;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NFAState nfaState = (NFAState) o;
        return id == nfaState.id;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
    
    @Override
    public String toString() {
        return "S" + id + (accepting ? "(accept)" : "");
    }
}
