package eu.cinik.colonqueryparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;
import java.util.stream.IntStream;

public class ParserTest {

    private void assertPolishNotation(int expectedSize, List<String> polishNotation, String... expected) {
        Assert.assertEquals("size", expectedSize, polishNotation.size());
        IntStream.range(0, expected.length).forEach((idx) -> {
            Assert.assertEquals("value at " + idx, expected[idx], polishNotation.get(idx));
        });
    }

    void assertProgram(String program, String... expected) {
        try (StringReader r = new StringReader(program)) {
            Parser.Node node = new Parser(r).statement();
            List<String> polish = Parser.toPolishNotation(node);
            assertPolishNotation(expected.length, polish, expected);
        }
    }

    void assertParsingNoException(String program) {
        try (StringReader r = new StringReader(program)) {
            new Parser(r).statement();
        }
    }

    void assertParseException(String expectedMessage, String program) {
        try (StringReader r = new StringReader(program)) {
            try {
                Parser.Node node = new Parser(r).statement();
            } catch (Parser.ParserException e) {
                Assert.assertEquals(expectedMessage, e.getMessage());
                return;
            }
        }
        Assert.fail("Expected exception");
    }

    @Test
    public void testText() {
        assertProgram("ABC", "ABC");
    }

    @Test
    public void testText2Statement() {
        assertProgram("ABC DEF", ",", "ABC", "DEF");
    }

    @Test
    public void testKeyValue() {
        assertProgram("KEY:VALUE", ":", "KEY", "VALUE");
    }

    @Test
    public void testNegKeyValue() {
        assertProgram("-KEY:VALUE", "-", ":", "KEY", "VALUE");
    }

    @Test
    public void testNegKeyAsText() {
        assertProgram("-", "-");
    }

    @Test
    public void testAND1() {
        assertProgram("t1 AND t2", "AND", "t1", "t2");
    }

    @Test
    public void testAND2() {
        assertProgram("t1 AND", "t1");
    }

    @Test
    public void testOR1() {
        assertProgram("t1 OR t2", "OR", "t1", "t2");
    }

    @Test
    public void testOR2() {
        assertProgram("t1 OR", "t1");
    }

    @Test
    public void testAND_OR1() {
        assertProgram("t1 OR t2 AND t3", "OR", "t1", "AND", "t2", "t3");
    }

    @Test
    public void testAND_OR2() {
        assertProgram("t1 AND t2 OR t3", "OR", "AND", "t1", "t2", "t3");
    }

    @Test
    public void testBracketed1() {
        assertProgram("(t1)", "t1");
    }

    @Test
    public void testBracketed2() {
        assertProgram("(t1 t2)", ",", "t1", "t2");
    }

    @Test
    public void testBracketed3() {
        assertProgram("(t1 AND t2)", "AND", "t1", "t2");
    }

    @Test
    public void testBracketed4() {
        assertProgram("(t1 OR t2)", "OR", "t1", "t2");
    }

    @Test
    public void testBracketed5() {
        assertProgram("t1 (t2 OR t3)", ",", "t1", "OR", "t2", "t3");
    }

    @Test
    public void testBracketed6() {
        assertProgram("(t1 OR t2) t3", ",", "OR", "t1", "t2", "t3");
    }

    @Test
    public void complexTest1() {
        assertProgram("(key1:value1 OR key2:value2) key3:value3", ",", "OR", ":", "key1", "value1", ":", "key2", "value2", ":", "key3", "value3");
    }

    @Test
    public void complexTest2() {
        assertProgram("(-key1:value1 OR -key2:value2) -key3:value3", ",", "OR", "-", ":", "key1", "value1", "-", ":", "key2", "value2", "-", ":", "key3", "value3");
    }

    @Test
    public void complexTest3() {
        assertParsingNoException("abc \"def and foo\" AND x OR y (-key1:value1 OR -key2:value2) -key3:value3");
    }

    @Test
    public void binaryComparision1() {
        assertProgram("t1>t2", ">", "t1", "t2");
    }

    @Test
    public void binaryComparision2() {
        assertProgram("t1<t2", "<", "t1", "t2");
    }

    @Test
    public void binaryComparision3() {
        assertProgram("t1=t2", "=", "t1", "t2");
    }

    @Test
    public void complexTest4() {
//        assertProgram("market = NASDAQ AND ticker=MSFT.NASDAQ");
        assertParsingNoException("t1 > t2 AND t3 < t4 OR (t5 = t6)");
    }


}
