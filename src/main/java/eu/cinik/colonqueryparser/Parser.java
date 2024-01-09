package eu.cinik.colonqueryparser;

import java.io.Reader;
import java.util.*;
import java.util.function.Supplier;

/**
 * <p>Query parser similar to that one of Google written in Java.
 * The parser accepts queries in a form like "key:value" and returns parsed lexem tree</p>
 *
 * <p>Programs are parsed character by character, so ensure that you use {@link java.io.BufferedReader} to speed up parsing</p>
 * <h2>Syntax: (examples of statements)</h2>
 * <ul>
 * <li><i>some text</i> - consists of 2 {@link TextToken}s: 'some' and 'text'</li>
 * <li><i>&quot;quoted string with a \&quot; inside&quot;</i> - quoted string</li>
 * <li><i>some_key:some_value</i> - {@link KeyValue}</li>
 * <li><i>some_key=some_value</i> - use of assignment operator {@link BinaryComparision.Operator}</li>
 * <li><i>some_key&lt;some_value</i> - use of inequality operator {@link BinaryComparision.Operator}</li>
 * <li><i>some_key&gt;some_value</i> - use of inequality operator {@link BinaryComparision.Operator}</li>
 * <li><i>-some_key:some_value</i> - negated {@link KeyValue}</li>
 * <li><i>key:value AND term OR something</i> - use of {@link AND}, {@link OR} operators</li>
 * <li><i>key:value AND (term OR something)</i> - use of parenthesis</li>
 * </ul>
 *
 * <p>
 *     A term represented by {@link TextToken} is any non-whitespace character string (as per {@link Character#isWhitespace(char)})
 * </p>
 * <p>
 *     All whitespaces (as per {@link Character#isWhitespace(char)}) except for quoted strings are ignored.
 * </p>
 * <p>
 *  Quoted strings is any text within double quotes. Any character inside double quotes can be quoted using backslash '\'
 * </p>
 *
 * <p> Example use:</p>
 * <pre>
 *     {@code
 *         String someTextToMatch = "some text to match abc";
 *         Map<String, String> dataToMatch = new HashMap<>();
 *         dataToMatch.put("key1", "value1");
 *         dataToMatch.put("key2", "value2");
 *         dataToMatch.put("key3", "value3");
 *         Deque<Boolean> calcStack = new LinkedList<>();
 *         try (StringReader r = new StringReader("abc \"def and foo\" AND x OR y (-key1:valuezz OR key2:value2) key3:value3")) {
 *             Deque<Parser.Node> stack = Parser.toStack(new Parser(r).statement());
 *             while (!stack.isEmpty()) {
 *                 Parser.Node node = stack.pop();
 *                 if (node instanceof Parser.Text) {
 *                     calcStack.push(someTextToMatch.contains(((Parser.Text) node).getText()));
 *                 } else if (node instanceof Parser.KeyValue) {
 *                     String key = ((Parser.KeyValue) node).getKey();
 *                     String value = ((Parser.KeyValue) node).getValue();
 *                     Boolean neg = ((Parser.KeyValue) node).isNeg();
 *                     calcStack.push(!neg ? Objects.equals(dataToMatch.get(key), value) : !Objects.equals(dataToMatch.get(key), value));
 *                 } else if (node instanceof Parser.AND) {
 *                     Boolean left = calcStack.pop();
 *                     Boolean right = calcStack.pop();
 *                     calcStack.push(left && right);
 *                 } else if (node instanceof Parser.OR) {
 *                     Boolean left = calcStack.pop();
 *                     Boolean right = calcStack.pop();
 *                     calcStack.push(left || right);
 *                 } else throw new RuntimeException("unknown token node");
 *             }
 *             System.out.println("result:" + calcStack);
 *         }
 *     }
 *     </pre>
 */
public class Parser {
    private Lexer lexer = new Lexer();
    private Token currentToken;
    private Reader reader;

    /**
     * Creates new parser for a program text to be read from supplied reader.
     * Programs are parsed character by character, so ensure that you use {@link java.io.BufferedReader} to speed up parsing
     *
     * @param reader the reader to read program to parse
     */
    public Parser(Reader reader) {
        this.reader = reader;
    }

    Token token() {
        if (currentToken == null) {
            currentToken = this.lexer.next(reader);
        }
        return currentToken;
    }

    <T extends Token> T consumeToken(Class<T> c) {
        if (currentToken == null) {
            currentToken = this.lexer.next(reader);
        }
        try {
            return (T) currentToken;
        } finally {
            this.currentToken = null;
        }
    }

    <T extends Token> T verify(Class<T> t, TokenType tokenType) {
        if (token() != null && token().getTokenType() == tokenType && token().getClass().equals(t)) {
            return (T) currentToken;
        } else return null;
    }

    <T extends Token> T consume(Class<T> t, TokenType tokenType) {
        if (token() != null && token().getTokenType() == tokenType && token().getClass().equals(t)) {
            return consumeToken(t);
        } else return null;
    }

    <T extends Token> T expect(Class<T> t, TokenType tokenType) {
        Token token = consumeToken(t);
        if (token == null)
            throw new ParserException(String.format("Expected token %s but nothing found", tokenType.toString()));
        else if (token.getTokenType() != tokenType)
            throw new ParserException(String.format("Expected token %s but %s found",
                    tokenType.toString(),
                    token.getTokenType().toString()));
        else if (!token.getClass().equals(t)) {
            throw new RuntimeException(String.format("Expected token %s but %s found",
                    tokenType.toString(),
                    token.getClass().toString()));
        } else return (T) token;
    }

    /**
     * Parse a single statement and turn it into nodes tree.
     *
     * @return tree of parsed nodes
     */
    public Node statement() {
        List<Node> result = new ArrayList<>();
        Node n;
        while ((n = or()) != null) {
            result.add(n);
            if (currentToken.getTokenType() == TokenType.EOF) {
                break;
            }
        }
        if (result.size() == 1) {
            return result.get(0);
        } else if (result.isEmpty()) {
            return null;
        } else return new Statement(result);
    }

    Node bracketed() {
        if ((consume(Token.class, TokenType.OBRACKET)) != null) {
            Node stmt = statement();
            expect(Token.class, TokenType.CBRACKET);
            return stmt;
        } else return null;
    }


    Node or() {
        Node left;
        if ((left = and()) != null) {
            Text token;
            if ((token = verifyText()) != null && token.getText().equalsIgnoreCase("OR")) {
                expectText();
                Node right = and();
                if (right != null) {
                    return new OR(left, right);
                } else return left;
            }
        }
        return left;
    }

    Node and() {
        //it's a relaxed implementation - if there's no right side, then return left only
        Node left;
        if ((left = binaryComparision()) != null) {
            Text token;
            if ((token = verifyText()) != null && token.getText().equalsIgnoreCase("AND")) {
                expectText();
                Node right = binaryComparision();
                if (right != null) {
                    return new AND(left, right);
                } else return left;
            }
        }
        return left;
    }


    private static final Map<BinaryComparision.Operator, TokenType> binCompToToken = new HashMap<>();

    static {
        binCompToToken.put(BinaryComparision.Operator.EQ, TokenType.EQ);
        binCompToToken.put(BinaryComparision.Operator.LT, TokenType.LT);
        binCompToToken.put(BinaryComparision.Operator.HT, TokenType.HT);
    }

    Node binaryComparision() {
        return binaryComparision(BinaryComparision.Operator.LT, () ->
                binaryComparision(BinaryComparision.Operator.HT, () ->
                        binaryComparision(BinaryComparision.Operator.EQ, () -> factor())));
    }


    Node binaryComparision(BinaryComparision.Operator oper, Supplier<Node> leftRight) {
        //it's a relaxed implementation - if there's no right side, then return left only
        Node left;
        if ((left = leftRight.get()) != null) {
            TokenType tt = binCompToToken.get(oper);
            if ((verify(Token.class, tt)) != null) {
                expect(Token.class, tt);
                Node right = leftRight.get();
                if (right != null) {
                    return new BinaryComparision(oper, left, right);
                } else return left;
            }
        }
        return left;
    }

    Node keyValue() {
        Text key;
        consume(TextToken.class, TokenType.WHITESPACE);
        Token neg = consume(Token.class, TokenType.NEG);
        consume(TextToken.class, TokenType.WHITESPACE);
        if ((key = acceptText()) != null) {
            consume(TextToken.class, TokenType.WHITESPACE);
            if (consume(Token.class, TokenType.COLON) != null) {
                Text value = expectText();
                consume(TextToken.class, TokenType.WHITESPACE);
                return new KeyValue(neg != null, key.getText(), value.getText());
            } else {
                return key;
            }
        } else if (neg != null) return new Text("-");
        else return null;

    }

    Text expectText() {
        Text t = acceptText();
        if (t == null) {
            throw new ParserException("Expected text");
        }
        return t;
    }

    Text acceptText() {
        consume(TextToken.class, TokenType.WHITESPACE);
        TextToken textToken;
        if ((textToken = consume(TextToken.class, TokenType.TEXTTOKEN)) != null) {
            consume(TextToken.class, TokenType.WHITESPACE);
            return new Text(textToken.getText());
        } else return null;
    }

    Text verifyText() {
        consume(TextToken.class, TokenType.WHITESPACE);
        TextToken textToken;
        if ((textToken = verify(TextToken.class, TokenType.TEXTTOKEN)) != null) {
            return new Text(textToken.getText());
        } else return null;
    }

    Node factor() {
        Node n;
        consume(TextToken.class, TokenType.WHITESPACE);
        if ((n = keyValue()) != null) {
            return n;
        } else if ((n = bracketed()) != null) {
            return n;
        } else return null;
        //throw new ParserException("invalid token " + this.currentToken);
    }

    /**
     * Generic exception for all parsing issues
     */
    static public class ParserException extends RuntimeException {
        public ParserException(String message) {
            super(message);
        }
    }

    /**
     * Converts parsed node tree to a stack
     *
     * @param node node tree
     * @return stack
     */
    static public Deque<Node> toStack(Node node) {
        Deque<Node> stack = new LinkedList<>();
        Parser.NodeVisitor visitor = new Parser.NodeVisitor() {

            @Override
            public void visit(Parser.AND and) {
                stack.push(and);
            }

            @Override
            public void visit(Parser.OR or) {
                stack.push(or);
            }

            @Override
            public void visit(Parser.KeyValue keyValue) {
                stack.push(keyValue);
            }

            @Override
            public void visit(Parser.Text text) {
                stack.push(text);
            }

            @Override
            public void visit(Parser.Statement statement) {
                //nothing
            }

            @Override
            public void visit(Parser.BinaryComparision binaryComparision) {
                stack.push(binaryComparision);
            }
        };
        node.visit(visitor);
        return stack;
    }

    /**
     * Converts parsed node tree to <a href="https://en.wikipedia.org/wiki/Reverse_Polish_notation">polish notation</a> where each term is represented by a single string in the list
     *
     * @param node
     * @return
     */
    static public List<String> toPolishNotation(Node node) {
        List<String> result = new ArrayList<>();
        node.visit(new NodeVisitor() {
            @Override
            public void visit(AND and) {
                result.add("AND");
            }

            @Override
            public void visit(OR or) {
                result.add("OR");
            }

            @Override
            public void visit(KeyValue keyValue) {
                if (keyValue.isNeg()) result.add("-");
                result.add(":");
                result.add(keyValue.key);
                result.add(keyValue.value);
            }

            @Override
            public void visit(Text text) {
                result.add(text.getText());
            }

            @Override
            public void visit(Statement statement) {
                result.add(",");
            }

            @Override
            public void visit(BinaryComparision binaryComparision) {
                result.add(binaryComparision.operator.label);
            }
        });
        return result;
    }


    /**
     * A node which represents token in a statement
     */
    public interface Node {
        /**
         * Implementation of visitor pattern
         *
         * @param visitor visitor to be invoked on all nodes withing the token tree
         */
        void visit(NodeVisitor visitor);
    }

    /**
     * Implementation of visitor for {@link Node}s tree
     */
    public interface NodeVisitor {
        /**
         * Callback when visiting {@link AND} operator
         *
         * @param and operator
         */
        void visit(AND and);

        /**
         * Callback when visiting {@link OR} operator
         *
         * @param or operator
         */
        void visit(OR or);

        /**
         * Key value with optional negation
         *
         * @param keyValue key value
         */
        void visit(KeyValue keyValue);

        /**
         * A term represented by {@link TextToken} is any non-whitespace character string (as per {@link Character#isWhitespace(char)})
         *
         * @param text
         */
        void visit(Text text);

        /**
         * Statement
         *
         * @param statement
         */
        void visit(Statement statement);

        void visit(BinaryComparision binaryComparision);
    }

    /**
     * Binary operator
     */
    static abstract class Binary implements Node {
        protected Node left;
        protected Node right;

        public Binary(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        public Node getLeft() {
            return left;
        }

        public Node getRight() {
            return right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Binary and = (Binary) o;
            return Objects.equals(left, and.left) && Objects.equals(right, and.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }


    }

    /**
     * Logical AND operator
     */
    static public class AND extends Binary {
        AND(Node left, Node right) {
            super(left, right);
        }

        @Override
        public void visit(NodeVisitor visitor) {
            visitor.visit(this);
            left.visit(visitor);
            right.visit(visitor);
        }

        @Override
        public String toString() {
            return "AND";
        }
    }

    /**
     * Logical OR operator
     */
    static public class OR extends Binary {
        OR(Node left, Node right) {
            super(left, right);
        }

        @Override
        public void visit(NodeVisitor visitor) {
            visitor.visit(this);
            left.visit(visitor);
            right.visit(visitor);
        }

        @Override
        public String toString() {
            return "OR";
        }
    }

    /**
     * Comparison operator
     */
    static public class BinaryComparision extends Binary {
        public enum Operator {
            /**
             * in-equality operator &gt;
             */
            HT(">"),
            /**
             * in-equality operator &lt;
             */
            LT("<"),
            /**
             * equality operator '='
             */
            EQ("=");
            private final String label;

            Operator(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        private Operator operator;

        BinaryComparision(Operator operator, Node left, Node right) {
            super(left, right);
            this.operator = operator;
        }

        @Override
        public void visit(NodeVisitor visitor) {
            visitor.visit(this);
            left.visit(visitor);
            right.visit(visitor);
        }

        public Operator getOperator() {
            return operator;
        }

        @Override
        public String toString() {
            return operator.label;
        }
    }

    /**
     * A term represented by {@link TextToken} is any non-whitespace character string (as per {@link Character#isWhitespace(char)}).
     * Also quoted string without surrounding quotes.
     */
    static public class Text implements Node {
        private String text;

        Text(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public void visit(NodeVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * 'key:value' with optional negation '-key:value'
     */
    static public class KeyValue implements Node {
        private boolean neg;
        private String key;
        private String value;

        KeyValue(boolean neg, String key, String value) {
            this.neg = neg;
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public boolean isNeg() {
            return neg;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyValue keyValue = (KeyValue) o;
            return neg == keyValue.neg && Objects.equals(key, keyValue.key) && Objects.equals(value, keyValue.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(neg, key, value);
        }

        @Override
        public void visit(NodeVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            if (neg) b.append("-");
            b.append(key);
            b.append(":");
            b.append(value);
            return b.toString();
        }
    }

    /**
     * Statement is just a list of nodes
     */
    static public class Statement implements Node {
        private List<Node> nodes;

        public Statement(List<Node> nodes) {
            this.nodes = nodes;
        }

        public List<Node> getNodes() {
            return nodes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Statement statement = (Statement) o;
            return Objects.equals(nodes, statement.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodes);
        }

        @Override
        public void visit(NodeVisitor visitor) {
            visitor.visit(this);
            this.nodes.stream().forEach((n) -> n.visit(visitor));
        }
    }
}



