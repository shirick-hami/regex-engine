package com.rickm.regex.engine.automaton;

import com.rickm.regex.engine.parser.AstNode;

/**
 * Non-deterministic Finite Automaton (NFA) for regex matching.
 * 
 * Built using Thompson's construction algorithm which converts
 * a regex AST into an NFA with the following properties:
 * - Single start state
 * - Single accepting state
 * - No transitions out of the accepting state
 * - At most two transitions out of any state
 */
public class NFA {
    
    private final NFAState start;
    private final NFAState accept;
    
    public NFA(NFAState start, NFAState accept) {
        this.start = start;
        this.accept = accept;
        this.accept.setAccepting(true);
    }
    
    public NFAState getStart() {
        return start;
    }
    
    public NFAState getAccept() {
        return accept;
    }
    
    /**
     * Builds an NFA from an AST using Thompson's construction.
     */
    public static NFA fromAST(AstNode ast) {
        NFAState.resetIdCounter();
        return build(ast);
    }
    
    private static NFA build(AstNode ast) {
        if (ast == null) {
            // Empty pattern - matches empty string
            NFAState start = new NFAState();
            NFAState accept = new NFAState();
            start.addEpsilonTransition(accept);
            return new NFA(start, accept);
        }
        
        switch (ast.getType()) {
            case LITERAL:
                return buildLiteral(ast);
            case ESCAPED:
                return buildEscaped(ast);
            case TAB:
                return buildTab();
            case WHITESPACE:
                return buildWhitespace();
            case ANY_CHAR:
                return buildAnyChar();
            case CHAR_CLASS:
                return buildCharClass(ast);
            case NEGATED_CHAR_CLASS:
                return buildNegatedCharClass(ast);
            case CONCAT:
                return buildConcat(ast);
            case ALTERNATION:
                return buildAlternation(ast);
            case STAR:
                return buildStar(ast);
            case PLUS:
                return buildPlus(ast);
            case QUESTION:
                return buildQuestion(ast);
            case GROUP:
                return build(ast.getFirstChild());
            default:
                throw new IllegalArgumentException("Unknown AST node type: " + ast.getType());
        }
    }
    
    /**
     * Builds NFA for a literal character.
     * 
     *   start --[c]--> accept
     */
    private static NFA buildLiteral(AstNode ast) {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        
        char c = ast.getCharacter();
        if (c == '\0') {
            // Empty/null matches empty string
            start.addEpsilonTransition(accept);
        } else {
            start.addTransition(c, accept);
        }
        
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for an escaped character.
     */
    private static NFA buildEscaped(AstNode ast) {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        start.addTransition(ast.getCharacter(), accept);
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for tab character.
     */
    private static NFA buildTab() {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        start.addTransition('\t', accept);
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for whitespace.
     */
    private static NFA buildWhitespace() {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        start.addWhitespaceTransition(accept);
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for any character (dot).
     */
    private static NFA buildAnyChar() {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        start.addAnyCharTransition(accept);
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for character class [abc].
     */
    private static NFA buildCharClass(AstNode ast) {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        start.addCharClassTransition(ast.getCharSet(), accept);
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for negated character class [^abc].
     */
    private static NFA buildNegatedCharClass(AstNode ast) {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        start.addNegatedCharClassTransition(ast.getCharSet(), accept);
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for concatenation AB.
     * 
     *   A.start --> A.accept --ε--> B.start --> B.accept
     */
    private static NFA buildConcat(AstNode ast) {
        if (ast.getChildren().isEmpty()) {
            NFAState start = new NFAState();
            NFAState accept = new NFAState();
            start.addEpsilonTransition(accept);
            return new NFA(start, accept);
        }
        
        NFA result = build(ast.getChildren().get(0));
        
        for (int i = 1; i < ast.getChildren().size(); i++) {
            NFA next = build(ast.getChildren().get(i));
            result.accept.setAccepting(false);
            result.accept.addEpsilonTransition(next.start);
            result = new NFA(result.start, next.accept);
        }
        
        return result;
    }
    
    /**
     * Builds NFA for alternation A|B.
     * 
     *            ε --> A --> ε
     *          /               \
     *   start                    --> accept
     *          \               /
     *            ε --> B --> ε
     */
    private static NFA buildAlternation(AstNode ast) {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        
        for (AstNode child : ast.getChildren()) {
            NFA childNFA = build(child);
            start.addEpsilonTransition(childNFA.start);
            childNFA.accept.setAccepting(false);
            childNFA.accept.addEpsilonTransition(accept);
        }
        
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for Kleene star A*.
     * 
     *         ε
     *         ↓
     *   start --> A.start --> A.accept --> accept
     *         ε                    ↑    ε
     *         └────────────────────┘
     *                   ε
     */
    private static NFA buildStar(AstNode ast) {
        NFA inner = build(ast.getFirstChild());
        
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        
        // Direct path for zero matches
        start.addEpsilonTransition(accept);
        
        // Path through the inner NFA
        start.addEpsilonTransition(inner.start);
        inner.accept.setAccepting(false);
        inner.accept.addEpsilonTransition(accept);
        
        // Loop back for multiple matches
        inner.accept.addEpsilonTransition(inner.start);
        
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for one-or-more A+.
     * 
     *   start --> A.start --> A.accept --> accept
     *                              ↑    ε
     *                              └────┘
     */
    private static NFA buildPlus(AstNode ast) {
        NFA inner = build(ast.getFirstChild());
        
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        
        start.addEpsilonTransition(inner.start);
        inner.accept.setAccepting(false);
        inner.accept.addEpsilonTransition(accept);
        
        // Loop back for multiple matches
        inner.accept.addEpsilonTransition(inner.start);
        
        return new NFA(start, accept);
    }
    
    /**
     * Builds NFA for optional A?.
     * 
     *         ε
     *         ↓
     *   start --> A.start --> A.accept --> accept
     *         └──────────────────────────→
     *                       ε
     */
    private static NFA buildQuestion(AstNode ast) {
        NFA inner = build(ast.getFirstChild());
        
        NFAState start = new NFAState();
        NFAState accept = new NFAState();
        
        // Path through inner NFA
        start.addEpsilonTransition(inner.start);
        inner.accept.setAccepting(false);
        inner.accept.addEpsilonTransition(accept);
        
        // Direct path for zero matches
        start.addEpsilonTransition(accept);
        
        return new NFA(start, accept);
    }
}
