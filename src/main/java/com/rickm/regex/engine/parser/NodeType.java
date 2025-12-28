package com.rickm.regex.engine.parser;

/**
 * Enumeration of all Abstract Syntax Tree (AST) node types
 * supported by the regex engine.
 */
public enum NodeType {
    /** Matches a single literal character */
    LITERAL,

    /** Matches any printable Unicode character (.) */
    ANY_CHAR,

    /** Matches a tab character (\t) */
    TAB,

    /** Matches any whitespace character (\s) */
    WHITESPACE,

    /** Matches any character in a character class [abc] */
    CHAR_CLASS,

    /** Matches any character NOT in a negated character class [^abc] */
    NEGATED_CHAR_CLASS,

    /** Concatenation of two expressions (sequence) */
    CONCAT,

    /** Alternation between two expressions (|) */
    ALTERNATION,

    /** Zero or more repetitions (*) */
    STAR,

    /** One or more repetitions (+) */
    PLUS,

    /** Zero or one occurrence (?) */
    QUESTION,

    /** Grouping expression (...) */
    GROUP,

    /** An escaped character (\x) */
    ESCAPED
}
