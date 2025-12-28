package com.rickm.regex.engine.automaton;

import java.util.*;

/**
 * Represents a state in a Deterministic Finite Automaton (DFA).
 * 
 * A DFA state has exactly one transition for each possible input symbol,
 * and no epsilon transitions. Each DFA state corresponds to a set of NFA states.
 */
public class DFAState {
    
    private static int nextId = 0;
    
    private final int id;
    private final Set<NFAState> nfaStates;
    private boolean accepting;
    
    /** Transitions indexed by character */
    private final Map<Character, DFAState> transitions;
    
    /** Character class transitions (for efficiency) */
    private final List<CharClassTransition> charClassTransitions;
    
    /** Default transition for characters not explicitly mapped */
    private DFAState defaultTransition;
    
    public DFAState(Set<NFAState> nfaStates) {
        this.id = nextId++;
        this.nfaStates = new HashSet<>(nfaStates);
        this.accepting = nfaStates.stream().anyMatch(NFAState::isAccepting);
        this.transitions = new HashMap<>();
        this.charClassTransitions = new ArrayList<>();
    }
    
    public static void resetIdCounter() {
        nextId = 0;
    }
    
    public int getId() {
        return id;
    }
    
    public Set<NFAState> getNfaStates() {
        return Collections.unmodifiableSet(nfaStates);
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
    public void addTransition(char c, DFAState target) {
        transitions.put(c, target);
    }
    
    /**
     * Adds a character class transition.
     */
    public void addCharClassTransition(Set<Character> chars, boolean negated, DFAState target) {
        charClassTransitions.add(new CharClassTransition(chars, negated, target));
    }
    
    /**
     * Sets the default transition (for any char).
     */
    public void setDefaultTransition(DFAState target) {
        this.defaultTransition = target;
    }
    
    /**
     * Gets the next state for a given input character.
     * Returns null if no transition exists (dead state).
     */
    public DFAState getTransition(char c) {
        // Check direct transitions first
        DFAState direct = transitions.get(c);
        if (direct != null) {
            return direct;
        }
        
        // Check character class transitions
        for (CharClassTransition cct : charClassTransitions) {
            if (cct.matches(c)) {
                return cct.target;
            }
        }
        
        // Fall back to default transition
        return defaultTransition;
    }
    
    public Map<Character, DFAState> getTransitions() {
        return Collections.unmodifiableMap(transitions);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFAState dfaState = (DFAState) o;
        return Objects.equals(nfaStates, dfaState.nfaStates);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nfaStates);
    }
    
    @Override
    public String toString() {
        return "D" + id + (accepting ? "(accept)" : "") + nfaStates;
    }
    
    /**
     * Represents a character class transition.
     */
    public static class CharClassTransition {
        final Set<Character> chars;
        final boolean negated;
        final DFAState target;
        
        public CharClassTransition(Set<Character> chars, boolean negated, DFAState target) {
            this.chars = chars;
            this.negated = negated;
            this.target = target;
        }
        
        public boolean matches(char c) {
            if (c == '\n' || c == '\r') {
                return false; // Never match line terminators in negated classes
            }
            boolean inSet = chars.contains(c);
            return negated ? !inSet : inSet;
        }
    }
}
