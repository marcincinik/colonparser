Query parser similar to that one of Google written in Java.
The parser accepts queries in a form like "key:value" and returns parsed lexem tree.

Example query can be like this
```
abc def AND x OR y (-key1:value1 OR -key2:value2) -key3:value3
```

and example code to parse it and put all conditions on a stack can be like below

```
package eu.cinik.colonparser.example;


import eu.cinik.colonparser.Parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.Deque;
import java.util.LinkedList;

public class Example {

    public static void main(String[] args) throws IOException {
        try (StringReader r = new StringReader("abc def AND x OR y (-key1:value1 OR -key2:value2) -key3:value3")) {
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
            };
            node.visit(visitor);
            while (!stack.isEmpty()) {
                System.out.println(stack.pop());
            }
        }

    }
}
```
