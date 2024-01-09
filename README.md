Query parser similar to that one of Google written in Java. The parser accepts queries in a form like "key:value" and returns parsed lexem tree





Programs are parsed character by character, so ensure that you use {@link java.io.BufferedReader} to speed up parsing

<h2>Syntax: (examples of statements)</h2> <ul> <li><i>some text</i> - consists of 2 {@link TextToken}s: 'some' and 'text'</li> <li><i>&quot;quoted string with a \&quot; inside&quot;</i> - quoted string</li> <li><i>some_key:some_value</i> - {@link KeyValue}</li> <li><i>some_key=some_value</i> - use of assignment operator {@link BinaryComparision.Operator}</li> <li><i>some_key&lt;some_value</i> - use of inequality operator {@link BinaryComparision.Operator}</li> <li><i>some_key&gt;some_value</i> - use of inequality operator {@link BinaryComparision.Operator}</li> <li><i>-some_key:some_value</i> - negated {@link KeyValue}</li> <li><i>key:value AND term OR something</i> - use of {@link AND}, {@link OR} operators</li> <li><i>key:value AND (term OR something)</i> - use of parenthesis</li> </ul>



A term represented by {@link TextToken} is any non-whitespace character string (as per {@link Character#isWhitespace(char)})



All whitespaces (as per {@link Character#isWhitespace(char)}) except for quoted strings are ignored.



Quoted strings is any text within double quotes. Any character inside double quotes can be quoted using backslash '\'





Example use:

<pre> {@code String someTextToMatch = "some text to match abc"; Map<String, String> dataToMatch = new HashMap<>(); dataToMatch.put("key1", "value1"); dataToMatch.put("key2", "value2"); dataToMatch.put("key3", "value3"); Deque<Boolean> calcStack = new LinkedList<>(); try (StringReader r = new StringReader("abc \"def and foo\" AND x OR y (-key1:valuezz OR key2:value2) key3:value3")) { Deque<Parser.Node> stack = Parser.toStack(new Parser(r).statement()); while (!stack.isEmpty()) { Parser.Node node = stack.pop(); if (node instanceof Parser.Text) { calcStack.push(someTextToMatch.contains(((Parser.Text) node).getText())); } else if (node instanceof Parser.KeyValue) { String key = ((Parser.KeyValue) node).getKey(); String value = ((Parser.KeyValue) node).getValue(); Boolean neg = ((Parser.KeyValue) node).isNeg(); calcStack.push(!neg ? Objects.equals(dataToMatch.get(key), value) : !Objects.equals(dataToMatch.get(key), value)); } else if (node instanceof Parser.AND) { Boolean left = calcStack.pop(); Boolean right = calcStack.pop(); calcStack.push(left && right); } else if (node instanceof Parser.OR) { Boolean left = calcStack.pop(); Boolean right = calcStack.pop(); calcStack.push(left || right); } else throw new RuntimeException("unknown token node"); } System.out.println("result:" + calcStack); } } </pre>

## `public Parser(Reader reader)`

Creates new parser for a program text to be read from supplied reader. Programs are parsed character by character, so ensure that you use {@link java.io.BufferedReader} to speed up parsing

* **Parameters:** `reader` — the reader to read program to parse

## `public Node statement()`

Parse a single statement and turn it into nodes tree.

* **Returns:** tree of parsed nodes

## `static public class ParserException extends RuntimeException`

Generic exception for all parsing issues

## `static public Deque<Node> toStack(Node node)`

Converts parsed node tree to a stack

* **Parameters:** `node` — node tree
* **Returns:** stack

## `static public List<String> toPolishNotation(Node node)`

Converts parsed node tree to <a href="https://en.wikipedia.org/wiki/Reverse_Polish_notation">polish notation</a> where each term is represented by a single string in the list

* **Parameters:** `node` —
* **Returns:**

## `public interface Node`

A node which represents token in a statement

## `void visit(NodeVisitor visitor)`

Implementation of visitor pattern

* **Parameters:** `visitor` — visitor to be invoked on all nodes withing the token tree

## `public interface NodeVisitor`

Implementation of visitor for {@link Node}s tree

## `void visit(AND and)`

Callback when visiting {@link AND} operator

* **Parameters:** `and` — operator

## `void visit(OR or)`

Callback when visiting {@link OR} operator

* **Parameters:** `or` — operator

## `void visit(KeyValue keyValue)`

Key value with optional negation

* **Parameters:** `keyValue` — key value

## `void visit(Text text)`

A term represented by {@link TextToken} is any non-whitespace character string (as per {@link Character#isWhitespace(char)})

* **Parameters:** `text` —

## `void visit(Statement statement)`

Statement

* **Parameters:** `statement` —

## `static abstract class Binary implements Node`

Binary operator

## `static public class AND extends Binary`

Logical AND operator

## `static public class OR extends Binary`

Logical OR operator

## `static public class BinaryComparision extends Binary`

Comparison operator

## `HT(">"),`

in-equality operator &gt;

## `LT("<"),`

in-equality operator &lt;

## `EQ("=")`

equality operator '='

## `static public class Text implements Node`

A term represented by {@link TextToken} is any non-whitespace character string (as per {@link Character#isWhitespace(char)}). Also quoted string without surrounding quotes.

## `static public class KeyValue implements Node`

'key:value' with optional negation '-key:value'

## `static public class Statement implements Node`

Statement is just a list of nodes