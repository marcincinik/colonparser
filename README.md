Query parser similar to that one of Google written in Java. The parser accepts queries in a form like "key:value" and
returns parsed lexem tree.

Example query can be like this

```
abc def AND x OR y (-key1:value1 OR -key2:value2) -key3:value3
```

Example calculator based on this parser:

```
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

```