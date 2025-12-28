package com.rickm.regex.engine.parser;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a node in the Abstract Syntax Tree (AST) of a parsed regular expression.
 *
 * The AST represents the hierarchical structure of the regex pattern,
 * where each node type corresponds to a regex construct.
 */
@Getter
@Setter
public class AstNode {

    /** The type of this AST node */
    private final NodeType type;

    /** Character value for LITERAL, ESCAPED, TAB nodes */
    private Character character;

    /** Set of characters for CHAR_CLASS and NEGATED_CHAR_CLASS nodes */
    private Set<Character> charSet;

    /** Child nodes (for CONCAT, ALTERNATION, quantifiers, GROUP) */
    private List<AstNode> children;

    /** Whether this node represents a negated character class */
    private boolean negated;

    /**
     * Creates a new AST node with the specified type.
     *
     * @param type the node type
     */
    public AstNode(NodeType type) {
        this.type = type;
        this.children = new ArrayList<>();
        this.negated = false;
    }

    /**
     * Creates a literal character node.
     *
     * @param ch the literal character
     * @return a new LITERAL node
     */
    public static AstNode literal(char ch) {
        AstNode node = new AstNode(NodeType.LITERAL);
        node.setCharacter(ch);
        return node;
    }

    /**
     * Creates an escaped character node.
     *
     * @param ch the escaped character
     * @return a new ESCAPED node
     */
    public static AstNode escaped(char ch) {
        AstNode node = new AstNode(NodeType.ESCAPED);
        node.setCharacter(ch);
        return node;
    }

    /**
     * Creates a tab character node.
     *
     * @return a new TAB node
     */
    public static AstNode tab() {
        AstNode node = new AstNode(NodeType.TAB);
        node.setCharacter('\t');
        return node;
    }

    /**
     * Creates a whitespace matching node.
     *
     * @return a new WHITESPACE node
     */
    public static AstNode whitespace() {
        return new AstNode(NodeType.WHITESPACE);
    }

    /**
     * Creates an any-character (dot) node.
     *
     * @return a new ANY_CHAR node
     */
    public static AstNode anyChar() {
        return new AstNode(NodeType.ANY_CHAR);
    }

    /**
     * Creates a character class node.
     *
     * @param chars the set of characters in the class
     * @param negated true if this is a negated class [^...]
     * @return a new CHAR_CLASS or NEGATED_CHAR_CLASS node
     */
    public static AstNode charClass(Set<Character> chars, boolean negated) {
        AstNode node = new AstNode(negated ? NodeType.NEGATED_CHAR_CLASS : NodeType.CHAR_CLASS);
        node.setCharSet(chars);
        node.setNegated(negated);
        return node;
    }

    /**
     * Creates a concatenation node.
     *
     * @param left the left child
     * @param right the right child
     * @return a new CONCAT node
     */
    public static AstNode concat(AstNode left, AstNode right) {
        AstNode node = new AstNode(NodeType.CONCAT);
        node.addChild(left);
        node.addChild(right);
        return node;
    }

    /**
     * Creates an alternation (OR) node.
     *
     * @param left the left alternative
     * @param right the right alternative
     * @return a new ALTERNATION node
     */
    public static AstNode alternation(AstNode left, AstNode right) {
        AstNode node = new AstNode(NodeType.ALTERNATION);
        node.addChild(left);
        node.addChild(right);
        return node;
    }

    /**
     * Creates a quantifier node (*, +, ?).
     *
     * @param type the quantifier type (STAR, PLUS, or QUESTION)
     * @param child the node being quantified
     * @return a new quantifier node
     */
    public static AstNode quantifier(NodeType type, AstNode child) {
        AstNode node = new AstNode(type);
        node.addChild(child);
        return node;
    }

    /**
     * Creates a grouping node.
     *
     * @param child the grouped expression
     * @return a new GROUP node
     */
    public static AstNode group(AstNode child) {
        AstNode node = new AstNode(NodeType.GROUP);
        node.addChild(child);
        return node;
    }

    /**
     * Adds a child node to this node.
     *
     * @param child the child node to add
     */
    public void addChild(AstNode child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    /**
     * Gets the first child node.
     *
     * @return the first child, or null if no children
     */
    public AstNode getFirstChild() {
        return children.isEmpty() ? null : children.get(0);
    }

    /**
     * Gets the second child node (for binary operations).
     *
     * @return the second child, or null if fewer than two children
     */
    public AstNode getSecondChild() {
        return children.size() < 2 ? null : children.get(1);
    }

    /**
     * Checks if this node has any children.
     *
     * @return true if this node has at least one child
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AstNode{type=").append(type);
        if (character != null) {
            sb.append(", char='").append(character).append("'");
        }
        if (charSet != null && !charSet.isEmpty()) {
            sb.append(", charSet=").append(charSet);
        }
        if (!children.isEmpty()) {
            sb.append(", children=").append(children.size());
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AstNode astNode = (AstNode) o;
        return negated == astNode.negated &&
                type == astNode.type &&
                Objects.equals(character, astNode.character) &&
                Objects.equals(charSet, astNode.charSet) &&
                Objects.equals(children, astNode.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, character, charSet, children, negated);
    }
}
