# Regex Engine: Theory and Implementation

A deep dive into the internals of the regex engine, explaining the lexer, parser, and three matching algorithms in detail.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [The Lexer (Tokenizer)](#the-lexer-tokenizer)
4. [The Parser (AST Builder)](#the-parser-ast-builder)
5. [The Backtracking Matcher](#the-backtracking-matcher)
6. [The NFA Matcher (Thompson's Construction)](#the-nfa-matcher-thompsons-construction)
7. [The DFA Matcher (Subset Construction)](#the-dfa-matcher-subset-construction)
8. [Complexity Analysis](#complexity-analysis)
9. [References](#references)

---

## Overview

Regular expressions are a powerful tool for pattern matching in strings. This engine implements the classic pipeline for regex processing:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Pattern   │────▶│    Lexer    │────▶│   Parser    │────▶│   Matcher   │
│   String    │     │ (Tokenizer) │     │(AST Builder)│     │  (Engine)   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │                   │                   │
                           ▼                   ▼                   ▼
                      Token Stream      Abstract Syntax       Match Result
                                            Tree
```

**Example Flow:**

```
Pattern: "a(b|c)*d"

Lexer Output (Tokens):
  [CHAR 'a'] [LPAREN] [CHAR 'b'] [PIPE] [CHAR 'c'] [RPAREN] [STAR] [CHAR 'd']

Parser Output (AST):
  CONCATENATION
  ├── LITERAL 'a'
  ├── STAR
  │   └── GROUP
  │       └── ALTERNATION
  │           ├── LITERAL 'b'
  │           └── LITERAL 'c'
  └── LITERAL 'd'

Matcher: Tests input strings against this AST
```

---

## Architecture

### Project Structure

```
com.regex.engine
├── parser/
│   ├── RegexLexer.java      # Tokenizes the pattern string
│   └── RegexParser.java     # Builds AST from tokens
├── model/
│   ├── AstNode.java         # AST node representation
│   ├── NodeType.java        # Enum of node types
│   └── MatchResult.java     # Result of matching operations
├── automaton/
│   ├── NFAState.java        # NFA state with transitions
│   ├── NFA.java             # Thompson's NFA construction
│   ├── DFAState.java        # DFA state (set of NFA states)
│   └── DFA.java             # Subset construction
├── matcher/
│   ├── BacktrackingMatcher.java  # Recursive backtracking
│   ├── NFAMatcher.java           # NFA simulation
│   └── DFAMatcher.java           # DFA-based matching
└── service/
    └── RegexService.java    # High-level API
```

---

## The Lexer (Tokenizer)

### Purpose

The lexer (or tokenizer) is the first stage of pattern processing. It reads the raw pattern string character by character and produces a stream of **tokens** - meaningful units that the parser can understand.

### Token Types

```java
public enum TokenType {
    // Literals
    CHAR,           // Any literal character: a, b, 1, @, etc.
    
    // Metacharacters
    DOT,            // .  (matches any character)
    STAR,           // *  (zero or more)
    PLUS,           // +  (one or more)
    QUESTION,       // ?  (zero or one)
    PIPE,           // |  (alternation)
    
    // Grouping
    LPAREN,         // (
    RPAREN,         // )
    
    // Character Classes
    LBRACKET,       // [
    RBRACKET,       // ]
    CARET,          // ^ (negation in character class)
    HYPHEN,         // - (range in character class)
    
    // Special
    ESCAPE,         // \  (escape sequence start)
    END             // End of input
}
```

### Lexer Algorithm

The lexer uses a simple state machine with lookahead:

```
┌─────────────────────────────────────────────────────────────┐
│                     LEXER STATE MACHINE                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────┐                                                │
│  │  START  │                                                │
│  └────┬────┘                                                │
│       │                                                      │
│       ▼                                                      │
│  ┌─────────────┐    '\'     ┌───────────────┐               │
│  │ Read Char   │───────────▶│ Handle Escape │               │
│  └──────┬──────┘            └───────┬───────┘               │
│         │                           │                        │
│         │ metachar?                 │ \t, \s, \n, etc.       │
│         ▼                           ▼                        │
│  ┌─────────────┐            ┌───────────────┐               │
│  │ Emit Meta   │            │ Emit Special  │               │
│  │   Token     │            │    Token      │               │
│  └─────────────┘            └───────────────┘               │
│         │                           │                        │
│         └───────────┬───────────────┘                        │
│                     │                                        │
│                     ▼                                        │
│              ┌─────────────┐                                │
│              │ Next Char   │                                │
│              └─────────────┘                                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Key Methods

```java
public class RegexLexer {
    private final String pattern;
    private int position = 0;
    
    /**
     * Returns the next token from the pattern.
     */
    public Token nextToken() {
        if (position >= pattern.length()) {
            return new Token(TokenType.END, "");
        }
        
        char c = pattern.charAt(position++);
        
        switch (c) {
            case '.': return new Token(TokenType.DOT, ".");
            case '*': return new Token(TokenType.STAR, "*");
            case '+': return new Token(TokenType.PLUS, "+");
            case '?': return new Token(TokenType.QUESTION, "?");
            case '|': return new Token(TokenType.PIPE, "|");
            case '(': return new Token(TokenType.LPAREN, "(");
            case ')': return new Token(TokenType.RPAREN, ")");
            case '[': return new Token(TokenType.LBRACKET, "[");
            case ']': return new Token(TokenType.RBRACKET, "]");
            case '\\': return handleEscape();
            default:  return new Token(TokenType.CHAR, String.valueOf(c));
        }
    }
    
    /**
     * Handles escape sequences like \t, \n, \s, \., etc.
     */
    private Token handleEscape() {
        if (position >= pattern.length()) {
            throw new RegexParseException("Trailing backslash");
        }
        
        char escaped = pattern.charAt(position++);
        
        switch (escaped) {
            case 't': return new Token(TokenType.CHAR, "\t");      // Tab
            case 'n': return new Token(TokenType.CHAR, "\n");      // Newline
            case 'r': return new Token(TokenType.CHAR, "\r");      // Carriage return
            case 's': return new Token(TokenType.WHITESPACE, "\\s"); // Whitespace class
            // Escaped metacharacters become literals
            case '.': case '*': case '+': case '?':
            case '|': case '(': case ')': case '[': case ']':
            case '\\':
                return new Token(TokenType.CHAR, String.valueOf(escaped));
            default:
                return new Token(TokenType.CHAR, String.valueOf(escaped));
        }
    }
}
```

### Example: Tokenizing "a\+b*"

```
Input: "a\+b*"

Step 1: Read 'a' → Token(CHAR, "a")
Step 2: Read '\' → Enter escape mode
Step 3: Read '+' → Token(CHAR, "+")  [literal plus, not quantifier]
Step 4: Read 'b' → Token(CHAR, "b")
Step 5: Read '*' → Token(STAR, "*")
Step 6: End of input → Token(END, "")

Output: [CHAR 'a'] [CHAR '+'] [CHAR 'b'] [STAR] [END]
```

---

## The Parser (AST Builder)

### Purpose

The parser takes the token stream from the lexer and builds an **Abstract Syntax Tree (AST)** - a hierarchical representation of the regex pattern that captures its structure and operator precedence.

### Grammar

The parser implements this grammar using **recursive descent parsing**:

```
regex      → alternation

alternation → concatenation ('|' concatenation)*

concatenation → quantified+

quantified → atom ('*' | '+' | '?')?

atom       → CHAR
           | DOT
           | '(' regex ')'
           | '[' charclass ']'

charclass  → '^'? (CHAR | CHAR '-' CHAR)+
```

### Operator Precedence (Highest to Lowest)

| Precedence | Operator | Description |
|------------|----------|-------------|
| 1 (highest)| `()` | Grouping |
| 2 | `*`, `+`, `?` | Quantifiers |
| 3 | Concatenation | Implicit (adjacent atoms) |
| 4 (lowest) | `\|` | Alternation |

### AST Node Types

```java
public enum NodeType {
    LITERAL,          // Single character
    ANY_CHAR,         // . (dot)
    CHAR_CLASS,       // [abc]
    NEGATED_CLASS,    // [^abc]
    CONCATENATION,    // ab (sequence)
    ALTERNATION,      // a|b (choice)
    STAR,             // a* (zero or more)
    PLUS,             // a+ (one or more)
    QUESTION,         // a? (zero or one)
    GROUP,            // (a) (grouping)
    WHITESPACE,       // \s
    TAB               // \t
}
```

### Parser Implementation

```java
public class RegexParser {
    private final RegexLexer lexer;
    private Token currentToken;
    
    /**
     * Main entry point - parses complete regex.
     */
    public AstNode parse() {
        currentToken = lexer.nextToken();
        AstNode ast = parseAlternation();
        expect(TokenType.END);
        return ast;
    }
    
    /**
     * alternation → concatenation ('|' concatenation)*
     */
    private AstNode parseAlternation() {
        AstNode left = parseConcatenation();
        
        while (currentToken.getType() == TokenType.PIPE) {
            consume(TokenType.PIPE);
            AstNode right = parseConcatenation();
            left = new AstNode(NodeType.ALTERNATION, left, right);
        }
        
        return left;
    }
    
    /**
     * concatenation → quantified+
     */
    private AstNode parseConcatenation() {
        List<AstNode> nodes = new ArrayList<>();
        
        while (isAtomStart()) {
            nodes.add(parseQuantified());
        }
        
        if (nodes.isEmpty()) {
            return new AstNode(NodeType.LITERAL, ""); // Empty regex
        }
        
        // Build left-associative concatenation tree
        AstNode result = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            result = new AstNode(NodeType.CONCATENATION, result, nodes.get(i));
        }
        
        return result;
    }
    
    /**
     * quantified → atom ('*' | '+' | '?')?
     */
    private AstNode parseQuantified() {
        AstNode atom = parseAtom();
        
        if (currentToken.getType() == TokenType.STAR) {
            consume(TokenType.STAR);
            return new AstNode(NodeType.STAR, atom);
        } else if (currentToken.getType() == TokenType.PLUS) {
            consume(TokenType.PLUS);
            return new AstNode(NodeType.PLUS, atom);
        } else if (currentToken.getType() == TokenType.QUESTION) {
            consume(TokenType.QUESTION);
            return new AstNode(NodeType.QUESTION, atom);
        }
        
        return atom;
    }
    
    /**
     * atom → CHAR | DOT | '(' regex ')' | '[' charclass ']'
     */
    private AstNode parseAtom() {
        switch (currentToken.getType()) {
            case CHAR:
                String ch = currentToken.getValue();
                consume(TokenType.CHAR);
                return new AstNode(NodeType.LITERAL, ch);
                
            case DOT:
                consume(TokenType.DOT);
                return new AstNode(NodeType.ANY_CHAR);
                
            case LPAREN:
                consume(TokenType.LPAREN);
                AstNode inner = parseAlternation();
                expect(TokenType.RPAREN);
                consume(TokenType.RPAREN);
                return new AstNode(NodeType.GROUP, inner);
                
            case LBRACKET:
                return parseCharacterClass();
                
            default:
                throw new RegexParseException("Unexpected token: " + currentToken);
        }
    }
}
```

### Example: Parsing "a(b|c)*d"

```
Input: "a(b|c)*d"

Parsing Steps:
1. parseAlternation() called
2. parseConcatenation() called
   2.1 parseQuantified() → parseAtom() → LITERAL 'a'
   2.2 parseQuantified() → parseAtom() → GROUP
       2.2.1 parseAlternation() inside group
             → ALTERNATION(LITERAL 'b', LITERAL 'c')
       2.2.2 See STAR → wrap in STAR node
   2.3 parseQuantified() → parseAtom() → LITERAL 'd'
   2.4 Build CONCATENATION tree

Resulting AST:

            CONCATENATION
           /             \
    CONCATENATION      LITERAL 'd'
    /           \
LITERAL 'a'    STAR
                 |
               GROUP
                 |
            ALTERNATION
            /         \
      LITERAL 'b'   LITERAL 'c'
```

### Character Class Parsing

Character classes like `[a-zA-Z0-9_]` require special handling:

```java
private AstNode parseCharacterClass() {
    consume(TokenType.LBRACKET);
    
    boolean negated = false;
    if (currentToken.getType() == TokenType.CARET) {
        negated = true;
        consume(TokenType.CARET);
    }
    
    Set<Character> chars = new HashSet<>();
    Character rangeStart = null;
    
    while (currentToken.getType() != TokenType.RBRACKET) {
        if (currentToken.getType() == TokenType.CHAR) {
            char c = currentToken.getValue().charAt(0);
            
            // Check for range: a-z
            if (peek() == TokenType.HYPHEN && peekNext() == TokenType.CHAR) {
                rangeStart = c;
                consume(TokenType.CHAR);
                consume(TokenType.HYPHEN);
                char rangeEnd = currentToken.getValue().charAt(0);
                consume(TokenType.CHAR);
                
                // Add all characters in range
                for (char ch = rangeStart; ch <= rangeEnd; ch++) {
                    chars.add(ch);
                }
            } else {
                chars.add(c);
                consume(TokenType.CHAR);
            }
        }
    }
    
    consume(TokenType.RBRACKET);
    
    NodeType type = negated ? NodeType.NEGATED_CLASS : NodeType.CHAR_CLASS;
    return new AstNode(type, chars);
}
```

---

## The Backtracking Matcher

### Overview

The backtracking matcher is a classic regex matching algorithm that recursively tries to match the pattern against the input. When a match attempt fails, it **backtracks** to try alternative paths.

### Core Concept: Continuation-Passing Style (CPS)

This implementation uses **Continuation-Passing Style** to handle backtracking iteratively rather than recursively, preventing stack overflow on long inputs.

```
Traditional Recursive:
  match(pattern, input) calls match(subpattern, remaining_input)
  → Deep call stack on long inputs
  
Continuation-Passing Style:
  match(pattern, input, continuation)
  continuation = "what to do after this matches"
  → Controlled iteration with explicit backtrack points
```

### Algorithm Visualization

```
Pattern: a*b
Input: "aaab"

Matching Process:
                          
  ┌─────────────────────────────────────────────────┐
  │ Try a* greedily (match as many 'a' as possible) │
  └─────────────────────────────────────────────────┘
                          │
                          ▼
  Position: a a a b       (matched "aaa", now try 'b')
            ↑ ↑ ↑ ↑
            └─┴─┴─┘
            matched by a*
                          │
                          ▼
  ┌─────────────────────────────────────────────────┐
  │ 'b' at position 3 → MATCH!                      │
  └─────────────────────────────────────────────────┘

If no 'b' found, backtrack:
  
  Position: a a a c
            ↑ ↑ ↑
            └─┴─┘
            try with 2 'a's... still no 'b'
            
  Position: a a a c
            ↑ ↑
            └─┘
            try with 1 'a'... still no 'b'
            
  Position: a a a c
            ↑
            └
            try with 0 'a's... no 'a' at position 0
            
  → NO MATCH
```

### Key Data Structures

```java
/**
 * Represents a point where we can backtrack to try alternatives.
 */
private static class Choice {
    final AstNode node;        // The pattern node being matched
    final int inputPos;        // Position in input string
    final int state;           // State within this node (e.g., which alternative)
    
    // For quantifiers: how many times we've matched so far
    final int quantifierCount;
}

/**
 * Stack of backtrack points.
 */
private final Deque<Choice> choiceStack = new ArrayDeque<>();
```

### Match Algorithm

```java
public MatchResult matchFull(String input) {
    // Initialize
    choiceStack.clear();
    int position = 0;
    AstNode current = ast;
    
    while (true) {
        // Try to match current node at current position
        MatchAttempt attempt = tryMatch(current, input, position);
        
        if (attempt.succeeded) {
            position = attempt.newPosition;
            current = attempt.nextNode;
            
            if (current == null && position == input.length()) {
                // Matched entire pattern and consumed entire input!
                return MatchResult.match(input, pattern, 0, position);
            }
            
            if (current == null) {
                // Matched pattern but input remains - need to backtrack
                if (!backtrack()) {
                    return MatchResult.noMatch(input, pattern);
                }
            }
        } else {
            // Match failed - try backtracking
            if (!backtrack()) {
                return MatchResult.noMatch(input, pattern);
            }
        }
    }
}

/**
 * Backtrack to the most recent choice point and try the next alternative.
 */
private boolean backtrack() {
    while (!choiceStack.isEmpty()) {
        Choice choice = choiceStack.pop();
        backtrackCount++;
        
        // Try next alternative for this choice
        if (hasNextAlternative(choice)) {
            // Restore state and try next alternative
            restoreState(choice);
            return true;
        }
    }
    return false; // No more alternatives
}
```

### Handling Quantifiers

Quantifiers (`*`, `+`, `?`) are the main source of backtracking:

```java
private MatchAttempt matchStar(AstNode node, String input, int position) {
    // Greedy: try to match as many as possible, then backtrack if needed
    AstNode child = node.getChild();
    int count = 0;
    int currentPos = position;
    
    // Match as many as possible
    while (currentPos < input.length()) {
        MatchAttempt attempt = tryMatch(child, input, currentPos);
        if (!attempt.succeeded) break;
        
        // Push choice point: "we could match one fewer"
        choiceStack.push(new Choice(node, position, count));
        
        count++;
        currentPos = attempt.newPosition;
    }
    
    // Return position after greedy matching
    return new MatchAttempt(true, currentPos);
}
```

### Backtrack Limit Protection

To prevent **catastrophic backtracking** (exponential time on certain patterns), we limit the number of backtrack operations:

```java
private void checkBacktrackLimit() {
    if (backtrackCount > maxBacktrackLimit) {
        throw new BacktrackLimitExceededException(
            "Backtrack limit exceeded: " + backtrackCount + 
            " > " + maxBacktrackLimit
        );
    }
}
```

**Pathological Example:**

```
Pattern: a*a*a*a*b
Input: "aaaaaaaaaaaaaaaaaaaaaaaaa" (25 a's, no b)

Without limit: Tries 2^25 = 33 million combinations!
With limit: Fails fast after 100,000 backtracks
```

---

## The NFA Matcher (Thompson's Construction)

### Overview

The NFA (Non-deterministic Finite Automaton) matcher uses **Thompson's Construction** to build an NFA from the regex, then simulates all possible paths simultaneously.

### Key Insight

Instead of backtracking, we track **all possible states** at once:

```
Backtracking:          NFA Simulation:
Try path 1             Track ALL paths
  Failed? → Backtrack    simultaneously
Try path 2             No backtracking
  Failed? → Backtrack    needed!
Try path 3
  ...
```

### Thompson's Construction

Each regex construct becomes a small NFA fragment:

#### Literal Character

```
Pattern: 'a'

    ┌───┐  a   ┌───┐
───▶│ 1 │─────▶│ 2 │
    └───┘      └───┘
    start      accept
```

#### Concatenation (AB)

```
Pattern: 'ab'

    ┌───┐  a   ┌───┐  ε   ┌───┐  b   ┌───┐
───▶│ 1 │─────▶│ 2 │─────▶│ 3 │─────▶│ 4 │
    └───┘      └───┘      └───┘      └───┘
                           ▲
                    Connect A's accept to B's start
                    via epsilon transition
```

#### Alternation (A|B)

```
Pattern: 'a|b'

              ε   ┌───┐  a   ┌───┐  ε
         ┌───────▶│ 2 │─────▶│ 3 │───────┐
         │        └───┘      └───┘       │
    ┌───┐│                               ▼┌───┐
───▶│ 1 │                                 │ 6 │
    └───┘│                               ▲└───┘
         │        ┌───┐  b   ┌───┐       │
         └───────▶│ 4 │─────▶│ 5 │───────┘
              ε   └───┘      └───┘  ε
```

#### Kleene Star (A*)

```
Pattern: 'a*'

         ε (skip)
    ┌─────────────────────┐
    │                     │
    ▼                     │
    ┌───┐  ε   ┌───┐  a   ┌───┐  ε   ┌───┐
───▶│ 1 │─────▶│ 2 │─────▶│ 3 │─────▶│ 4 │
    └───┘      └───┘      └───┘      └───┘
                 ▲                     │
                 │         ε           │
                 └─────────────────────┘
                      (loop back)
```

### NFA State Implementation

```java
public class NFAState {
    private final int id;
    private boolean accepting;
    
    // Transitions
    private final Map<Character, Set<NFAState>> charTransitions;
    private final Set<NFAState> epsilonTransitions;
    private final Set<NFAState> anyCharTransitions;  // For '.'
    private final Set<Character> charClassChars;      // For [abc]
    private boolean charClassNegated;
    
    /**
     * Get all states reachable on input character c.
     */
    public Set<NFAState> getTransitions(char c) {
        Set<NFAState> result = new HashSet<>();
        
        // Direct character transition
        Set<NFAState> direct = charTransitions.get(c);
        if (direct != null) {
            result.addAll(direct);
        }
        
        // Any character (.) transition - matches if not newline
        if (!anyCharTransitions.isEmpty() && c != '\n') {
            result.addAll(anyCharTransitions);
        }
        
        // Character class transitions
        if (charClassChars != null) {
            boolean inClass = charClassChars.contains(c);
            if (charClassNegated ? !inClass : inClass) {
                result.addAll(charClassTransitions);
            }
        }
        
        return result;
    }
}
```

### NFA Construction from AST

```java
public class NFA {
    private NFAState start;
    private NFAState accept;
    
    public static NFA fromAST(AstNode node) {
        switch (node.getType()) {
            case LITERAL:
                return createLiteral(node.getValue().charAt(0));
                
            case ANY_CHAR:
                return createAnyChar();
                
            case CONCATENATION:
                NFA left = fromAST(node.getLeft());
                NFA right = fromAST(node.getRight());
                return concatenate(left, right);
                
            case ALTERNATION:
                NFA option1 = fromAST(node.getLeft());
                NFA option2 = fromAST(node.getRight());
                return alternate(option1, option2);
                
            case STAR:
                NFA inner = fromAST(node.getChild());
                return star(inner);
                
            case PLUS:
                NFA innerPlus = fromAST(node.getChild());
                return plus(innerPlus);
                
            case QUESTION:
                NFA innerOpt = fromAST(node.getChild());
                return optional(innerOpt);
                
            // ... other cases
        }
    }
    
    private static NFA star(NFA inner) {
        NFAState newStart = new NFAState();
        NFAState newAccept = new NFAState();
        newAccept.setAccepting(true);
        inner.accept.setAccepting(false);
        
        // ε from new start to inner start (enter loop)
        newStart.addEpsilonTransition(inner.start);
        // ε from new start to new accept (skip entirely)
        newStart.addEpsilonTransition(newAccept);
        // ε from inner accept to inner start (repeat)
        inner.accept.addEpsilonTransition(inner.start);
        // ε from inner accept to new accept (exit loop)
        inner.accept.addEpsilonTransition(newAccept);
        
        return new NFA(newStart, newAccept);
    }
}
```

### NFA Simulation Algorithm

```java
public class NFAMatcher {
    
    public MatchResult matchFull(String input) {
        // Start with epsilon closure of start state
        Set<NFAState> currentStates = epsilonClosure(Set.of(nfa.getStart()));
        
        // Process each input character
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            // Compute next state set
            Set<NFAState> nextStates = new HashSet<>();
            for (NFAState state : currentStates) {
                nextStates.addAll(state.getTransitions(c));
            }
            
            // Take epsilon closure
            currentStates = epsilonClosure(nextStates);
            
            if (currentStates.isEmpty()) {
                return MatchResult.noMatch(input, pattern);
            }
        }
        
        // Check if any current state is accepting
        boolean matched = currentStates.stream()
            .anyMatch(NFAState::isAccepting);
            
        return matched 
            ? MatchResult.match(input, pattern, 0, input.length())
            : MatchResult.noMatch(input, pattern);
    }
    
    /**
     * Computes all states reachable via epsilon transitions.
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
}
```

### Visualization: NFA Simulation

```
Pattern: a*b
NFA:
         ε           ε
    ┌─────────┐ ┌─────────┐
    │         ▼ │         ▼
───▶●────ε───▶●────a───▶●────ε───▶●────b───▶◎
    1         2         3         4         5

Input: "aab"

Step 0: ε-closure({1}) = {1, 2, 4}
        States with accepting after 'b': none yet

Step 1: Input 'a'
        Move: {2} → 3 on 'a'
        ε-closure: {3, 2, 4}

Step 2: Input 'a'  
        Move: {2} → 3 on 'a'
        ε-closure: {3, 2, 4}

Step 3: Input 'b'
        Move: {4} → 5 on 'b'
        ε-closure: {5}
        
Step 4: State 5 is accepting → MATCH!
```

---

## The DFA Matcher (Subset Construction)

### Overview

The DFA (Deterministic Finite Automaton) matcher converts the NFA to a DFA, where each DFA state represents a **set of NFA states**. This gives guaranteed O(n) matching time.

### Key Insight: Subset Construction

```
NFA State Sets become DFA States:

NFA has states: {1, 2, 3, 4, 5}

DFA state A = NFA states {1, 2, 4}
DFA state B = NFA states {3, 2, 4}  
DFA state C = NFA states {5}

DFA transitions:
  A --a--> B
  B --a--> B
  B --b--> C  (C is accepting because it contains NFA accept state)
```

### Lazy DFA Construction

Instead of building the entire DFA upfront (which could be exponentially large), we build it **lazily** - computing states only when needed:

```java
public class DFAMatcher {
    private final Map<Set<NFAState>, DFAState> stateCache;
    private final DFAState startState;
    
    public DFAMatcher(AstNode ast, long timeoutMs) {
        this.nfa = NFA.fromAST(ast);
        this.stateCache = new HashMap<>();
        
        // Only create start state initially
        Set<NFAState> startNfaStates = epsilonClosure(Set.of(nfa.getStart()));
        this.startState = getOrCreateState(startNfaStates);
    }
    
    /**
     * Gets the next DFA state for input character c.
     * Computes new states on-demand (lazy construction).
     */
    private DFAState getNextState(DFAState current, char c) {
        // Check cache first
        DFAState cached = current.getTransition(c);
        if (cached != null) {
            return cached;
        }
        
        // Compute next NFA state set
        Set<NFAState> nextNfaStates = new HashSet<>();
        for (NFAState nfaState : current.getNfaStates()) {
            nextNfaStates.addAll(nfaState.getTransitions(c));
        }
        
        if (nextNfaStates.isEmpty()) {
            return null;  // Dead state
        }
        
        // Epsilon closure
        nextNfaStates = epsilonClosure(nextNfaStates);
        
        // Get or create DFA state
        DFAState nextState = getOrCreateState(nextNfaStates);
        
        // Cache for future use
        current.addTransition(c, nextState);
        
        return nextState;
    }
    
    /**
     * O(n) matching - just follow transitions.
     */
    public MatchResult matchFull(String input) {
        DFAState current = startState;
        
        for (int i = 0; i < input.length(); i++) {
            current = getNextState(current, input.charAt(i));
            if (current == null) {
                return MatchResult.noMatch(input, pattern);
            }
        }
        
        return current.isAccepting()
            ? MatchResult.match(input, pattern, 0, input.length())
            : MatchResult.noMatch(input, pattern);
    }
}
```

### DFA State Representation

```java
public class DFAState {
    private final Set<NFAState> nfaStates;
    private final boolean accepting;
    private final Map<Character, DFAState> transitions;
    
    public DFAState(Set<NFAState> nfaStates) {
        this.nfaStates = nfaStates;
        this.transitions = new HashMap<>();
        
        // DFA state is accepting if ANY NFA state is accepting
        this.accepting = nfaStates.stream()
            .anyMatch(NFAState::isAccepting);
    }
    
    public void addTransition(char c, DFAState target) {
        transitions.put(c, target);
    }
    
    public DFAState getTransition(char c) {
        return transitions.get(c);
    }
}
```

### Visualization: DFA Construction

```
Pattern: a*b

NFA (simplified):
    ●──ε──▶●──a──▶●──ε──┐
    1      2      3     │
    │                   ▼
    │              ●──b──▶◎
    │              4      5
    └──────ε──────▶┘

DFA Construction:

Step 1: Start state = ε-closure({1}) = {1, 2, 4}
        Is accepting? No (5 not in set)
        
Step 2: From {1,2,4} on 'a':
        Move: {2}→{3}
        ε-closure({3}) = {3, 2, 4}
        New state: {2, 3, 4}
        
Step 3: From {1,2,4} on 'b':
        Move: {4}→{5}
        ε-closure({5}) = {5}
        New state: {5} - ACCEPTING!
        
Step 4: From {2,3,4} on 'a':
        Same as step 2: {2, 3, 4}
        
Step 5: From {2,3,4} on 'b':
        Move: {4}→{5}
        State: {5} - already exists

Resulting DFA:

    ┌───────a──────┐
    │              │
    ▼              │
   [A]─────a─────▶[B]
 {1,2,4}        {2,3,4}
    │              │
    b              b
    │              │
    ▼              ▼
   [C]◀───────────┘
   {5}
 ACCEPT
```

### Benefits of Lazy Construction

1. **Memory Efficient**: Only creates states actually needed for the input
2. **Unicode Support**: No need to enumerate all possible characters
3. **Fast Startup**: Construction happens incrementally during matching
4. **Practical DFA Size**: Avoids exponential blowup for most patterns

---

## Complexity Analysis

### Time Complexity

| Operation | Backtracking | NFA | DFA |
|-----------|--------------|-----|-----|
| Construction | O(m) | O(m) | O(m) initial, O(2^m) worst |
| Matching | O(2^n) worst | O(n·m) | O(n) |
| Space | O(n) stack | O(m) states | O(2^m) states worst |

Where:
- n = input string length
- m = pattern length

### When to Use Each Engine

```
┌─────────────────────────────────────────────────────────────────┐
│                    CHOOSING AN ENGINE                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  User-provided pattern? ──Yes──▶ NFA (safe from ReDoS)         │
│         │                                                       │
│         No                                                      │
│         │                                                       │
│         ▼                                                       │
│  Same pattern, many inputs? ──Yes──▶ DFA (amortized O(n))     │
│         │                                                       │
│         No                                                      │
│         │                                                       │
│         ▼                                                       │
│  Simple pattern, short input? ──Yes──▶ Backtracking (simple)  │
│         │                                                       │
│         No                                                      │
│         │                                                       │
│         ▼                                                       │
│     NFA (safest default)                                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Pathological Cases

**Catastrophic Backtracking:**
```
Pattern: a*a*a*a*b
Input: "aaaaaaaaaaaaaaaaaaaaaaaaa" (no 'b')

Backtracking: O(2^25) ≈ 33 million operations
NFA: O(25 × 10) = 250 operations
DFA: O(25) = 25 operations
```

**DFA State Explosion:**
```
Pattern: .{10}.{10}.{10}
Theoretical DFA states: O(256^10) - impractical!

With lazy construction: Only creates states for 
characters actually in the input - usually small.
```

---

## References

1. **Thompson, Ken.** "Regular Expression Search Algorithm." *Communications of the ACM*, 1968.

2. **Aho, Sethi, Ullman.** "Compilers: Principles, Techniques, and Tools" (The Dragon Book). Chapter 3: Lexical Analysis.

3. **Cox, Russ.** "Regular Expression Matching Can Be Simple And Fast." https://swtch.com/~rsc/regexp/regexp1.html

4. **Friedl, Jeffrey.** "Mastering Regular Expressions." O'Reilly Media.

5. **Hopcroft, Motwani, Ullman.** "Introduction to Automata Theory, Languages, and Computation."

---

## Author

**Saptarick Mishra**

This implementation is part of the Regex Engine project, released under the MIT License.

---

*"Some people, when confronted with a problem, think 'I know, I'll use regular expressions.' Now they have two problems."* — Jamie Zawinski

*But with a good understanding of how regex engines work, you can solve both.* 😊
