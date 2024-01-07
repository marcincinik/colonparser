package eu.cinik.colonparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
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

    void assertProgram(String program, String... expected) throws IOException {
        try (StringReader r = new StringReader(program)) {
            Parser.Node node = new Parser(r).statement();
            List<String> polish = Parser.toPolishNotation(node);
            assertPolishNotation(expected.length, polish, expected);
        }
    }

    void assertParseException(String expectedMessage, String program) throws IOException {
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
    public void testText() throws Exception {
        assertProgram("ABC", "ABC");
    }

    @Test
    public void testText2Statement() throws Exception {
        assertProgram("ABC DEF", ",", "ABC", "DEF");
    }

    @Test
    public void testKeyValue() throws Exception {
        assertProgram("KEY:VALUE", ":", "KEY", "VALUE");
    }

    @Test
    public void testNegKeyValue() throws Exception {
        assertProgram("-KEY:VALUE", "-", ":", "KEY", "VALUE");
    }

    @Test
    public void testNegKeyAsText() throws Exception {
        assertProgram("-", "-");
    }

    @Test
    public void testAND1() throws Exception {
        assertProgram("t1 AND t2", "AND", "t1", "t2");
    }

    @Test
    public void testAND2() throws Exception {
        assertProgram("t1 AND", "t1");
    }

    @Test
    public void testOR1() throws Exception {
        assertProgram("t1 OR t2", "OR", "t1", "t2");
    }

    @Test
    public void testOR2() throws Exception {
        assertProgram("t1 OR", "t1");
    }

    @Test
    public void testAND_OR1() throws Exception {
        assertProgram("t1 OR t2 AND t3", "OR", "t1", "AND", "t2", "t3");
    }

    @Test
    public void testAND_OR2() throws Exception {
        assertProgram("t1 AND t2 OR t3", "OR", "AND", "t1", "t2", "t3");
    }

    @Test
    public void testBracketed1() throws Exception {
        assertProgram("(t1)", "t1");
    }

    @Test
    public void testBracketed2() throws Exception {
        assertProgram("(t1 t2)", ",", "t1", "t2");
    }

    @Test
    public void testBracketed3() throws Exception {
        assertProgram("(t1 AND t2)", "AND", "t1", "t2");
    }

    @Test
    public void testBracketed4() throws Exception {
        assertProgram("(t1 OR t2)", "OR", "t1", "t2");
    }

    @Test
    public void testBracketed5() throws Exception {
        assertProgram("t1 (t2 OR t3)", ",", "t1", "OR", "t2", "t3");
    }

    @Test
    public void testBracketed6() throws Exception {
        assertProgram("(t1 OR t2) t3", ",", "OR", "t1", "t2", "t3");
    }

    @Test
    public void complexTest1() throws Exception{
        assertProgram("(key1:value1 OR key2:value2) key3:value3", ",", "OR", ":", "key1", "value1",":","key2","value2", ":","key3", "value3");
    }

    @Test
    public void complexTest2() throws Exception{
        assertProgram("(-key1:value1 OR -key2:value2) -key3:value3", ",", "OR", "-",":", "key1", "value1","-",":","key2","value2", "-",":","key3", "value3");
    }

}
