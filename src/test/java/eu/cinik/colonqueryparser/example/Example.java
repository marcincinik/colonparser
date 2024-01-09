package eu.cinik.colonqueryparser.example;


import eu.cinik.colonqueryparser.Parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class Example {

    public static void main(String[] args) throws IOException {
        example2();

    }

    private static void example2() {
        String someTextToMatch = "some text to match abc";
        Map<String, String> dataToMatch = new HashMap<>();
        dataToMatch.put("key1", "value1");
        dataToMatch.put("key2", "value2");
        dataToMatch.put("key3", "value3");

        Deque<Boolean> calcStack = new LinkedList<>();
        try (StringReader r = new StringReader("abc \"def and foo\" AND x OR y (-key1:valuezz OR key2:value2) key3:value3")) {
            Deque<Parser.Node> stack = Parser.toStack(new Parser(r).statement());

            while (!stack.isEmpty()) {
                Parser.Node node = stack.pop();
                if (node instanceof Parser.Text) {
                    calcStack.push(someTextToMatch.contains(((Parser.Text) node).getText()));
                } else if (node instanceof Parser.KeyValue) {
                    String key = ((Parser.KeyValue) node).getKey();
                    String value = ((Parser.KeyValue) node).getValue();
                    Boolean neg = ((Parser.KeyValue) node).isNeg();
                    calcStack.push(!neg ? Objects.equals(dataToMatch.get(key), value) : !Objects.equals(dataToMatch.get(key), value));
                } else if (node instanceof Parser.AND) {
                    Boolean left = calcStack.pop();
                    Boolean right = calcStack.pop();
                    calcStack.push(left && right);
                } else if (node instanceof Parser.OR) {
                    Boolean left = calcStack.pop();
                    Boolean right = calcStack.pop();
                    calcStack.push(left || right);
                } else throw new RuntimeException("unknown token node");
            }
            System.out.println("result:" + calcStack);
        }
    }

    private static void example1() {
        try (StringReader r = new StringReader("abc \"def and foo\" AND x OR y (-key1:value1 OR -key2:value2) -key3:value3")) {
            Parser.Node node = new Parser(r).statement();
            Deque<String> stack = new LinkedList<>();
            Parser.NodeVisitor visitor = new Parser.NodeVisitor() {

                @Override
                public void visit(Parser.AND and) {
                    stack.push("AND");
                }

                @Override
                public void visit(Parser.OR or) {
                    stack.push("OR");
                }

                @Override
                public void visit(Parser.KeyValue keyValue) {
                    if (keyValue.isNeg()) stack.add("-");
                    stack.push(keyValue.getKey());
                    stack.push(keyValue.getValue());
                }

                @Override
                public void visit(Parser.Text text) {
                    stack.push(text.getText());
                }

                @Override
                public void visit(Parser.Statement statement) {
                    //nothing
                }

                @Override
                public void visit(Parser.BinaryComparision binaryComparision) {
                    stack.push(binaryComparision.getOperator().getLabel());
                }
            };
            node.visit(visitor);
            while (!stack.isEmpty()) {
                System.out.println(stack.pop());
            }
        }
    }
}
