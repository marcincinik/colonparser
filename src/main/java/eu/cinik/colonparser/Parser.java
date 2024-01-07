package eu.cinik.colonparser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Parser {
    private Lexer lexer = new Lexer();
    private Token currentToken;
    private Reader reader;

    Parser(Reader reader) {
        this.reader = reader;
    }

    Token token() throws IOException {
        if (currentToken == null) {
            currentToken = this.lexer.next(reader);
        }
        return currentToken;
    }

    <T extends Token> T consumeToken(Class<T> c) throws IOException {
        if (currentToken == null) {
            currentToken = this.lexer.next(reader);
        }
        try {
            return (T) currentToken;
        } finally {
            this.currentToken = null;
        }
    }

    <T extends Token> T verify(Class<T> t, TokenType tokenType) throws IOException {
        if (token() != null && token().getTokenType() == tokenType && token().getClass().equals(t)) {
            return (T) currentToken;
        } else return null;
    }

    <T extends Token> T consume(Class<T> t, TokenType tokenType) throws IOException {
        if (token() != null && token().getTokenType() == tokenType && token().getClass().equals(t)) {
            return consumeToken(t);
        } else return null;
    }

    <T extends Token> T expect(Class<T> t, TokenType tokenType) throws IOException {
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

    public Node statement() throws IOException {
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

    Node bracketed() throws IOException {
        if ((consume(Token.class, TokenType.OBRACKET)) != null) {
            Node stmt = statement();
            expect(Token.class, TokenType.CBRACKET);
            return stmt;
        } else return null;
    }


    Node or() throws IOException {
        Node left;
        if ((left = and()) != null) {
            Text orToken;
            if ((orToken = verifyText()) != null && orToken.getText().equalsIgnoreCase("OR")) {
                expectText();
                Node right = and();
                if (right != null) {
                    return new OR(left, right);
                } else return left;
            }
        }
        return left;
    }

    Node and() throws IOException {
        //it's a relaxed implementation - if there's no right side, then return left only
        Node left;
        if ((left = factor()) != null) {
            Text andToken;
            if ((andToken = verifyText()) != null && andToken.getText().equalsIgnoreCase("AND")) {
                expectText();
                Node right = factor();
                if (right != null) {
                    return new AND(left, right);
                } else return left;
            }
        }
        return left;
    }


    Node keyValue() throws IOException {
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

    Text expectText() throws IOException {
        Text t = acceptText();
        if (t == null) {
            throw new ParserException("Expected text");
        }
        return t;
    }

    Text acceptText() throws IOException {
        consume(TextToken.class, TokenType.WHITESPACE);
        TextToken textToken;
        if ((textToken = consume(TextToken.class, TokenType.TEXTTOKEN)) != null) {
            consume(TextToken.class, TokenType.WHITESPACE);
            return new Text(textToken.getText());
        } else return null;
    }

    Text verifyText() throws IOException {
        consume(TextToken.class, TokenType.WHITESPACE);
        TextToken textToken;
        if ((textToken = verify(TextToken.class, TokenType.TEXTTOKEN)) != null) {
            return new Text(textToken.getText());
        } else return null;
    }

    Node factor() throws IOException {
        Node n;
        consume(TextToken.class, TokenType.WHITESPACE);
        if ((n = keyValue()) != null) {
            return n;
        } else if ((n = bracketed()) != null) {
            return n;
        } else return null;
            //throw new ParserException("invalid token " + this.currentToken);
    }

    static public class ParserException extends RuntimeException {
        public ParserException(String message) {
            super(message);
        }
    }

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
        });
        return result;
    }


    public interface Node {
        void visit(NodeVisitor visitor);
    }

    public interface NodeVisitor {
        void visit(AND and);

        void visit(OR or);

        void visit(KeyValue keyValue);

        void visit(Text text);

        void visit(Statement statement);
    }

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

    static public class AND extends Binary {
        public AND(Node left, Node right) {
            super(left, right);
        }

        @Override
        public void visit(NodeVisitor visitor) {
            visitor.visit(this);
            left.visit(visitor);
            right.visit(visitor);
        }

    }

    static public class OR extends Binary {
        public OR(Node left, Node right) {
            super(left, right);
        }

        @Override
        public void visit(NodeVisitor visitor) {
            visitor.visit(this);
            left.visit(visitor);
            right.visit(visitor);
        }

    }

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
    }

    static public class KeyValue implements Node {
        private boolean neg;
        private String key;
        private String value;

        public KeyValue(boolean neg, String key, String value) {
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
    }

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



