package eu.cinik.colonqueryparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class LexerTest {

    @Test
    public void lexerEmptyInput() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("")) {
            Token t = l.next(r);
            Assert.assertEquals(new Token(TokenType.EOF), t);
        }
    }


    @Test
    public void lexerColon() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader(":")) {
            Token t = l.next(r);
            Assert.assertEquals(new Token(TokenType.COLON), t);
        }
    }

    @Test
    public void lexerStar() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("*")) {
            Token t = l.next(r);
            Assert.assertEquals(new Token(TokenType.STAR), t);
        }
    }


    @Test
    public void lexerNeg() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("-")) {
            Token t = l.next(r);
            Assert.assertEquals(new Token(TokenType.NEG), t);
        }
    }


    @Test
    public void lexerText1() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("A")) {
            Token t = l.next(r);
            Assert.assertEquals(new TextToken(TokenType.TEXTTOKEN, "A"), t);
        }
    }

    @Test
    public void lexerText2() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("AB")) {
            Token t = l.next(r);
            Assert.assertEquals(new TextToken(TokenType.TEXTTOKEN, "AB"), t);
        }
    }

    @Test
    public void lexerWhitespace1() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader(" ")) {
            Token t = l.next(r);
            Assert.assertEquals(new TextToken(TokenType.WHITESPACE, " "), t);
        }
    }

    @Test
    public void lexerWhitespace2() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("  ")) {
            Token t = l.next(r);
            Assert.assertEquals(new TextToken(TokenType.WHITESPACE, "  "), t);
        }
    }

    @Test
    public void lexerQuotedText1() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("\"\"")) {
            Token t = l.next(r);
            Assert.assertEquals(new TextToken(TokenType.TEXTTOKEN, ""), t);
        }
    }

    @Test
    public void lexerQuotedText2() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("\"A\"")) {
            Token t = l.next(r);
            Assert.assertEquals(new TextToken(TokenType.TEXTTOKEN, "A"), t);
        }
    }

    @Test
    public void lexerQuotedText3() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("\"AB\"")) {
            Token t = l.next(r);
            Assert.assertEquals(new TextToken(TokenType.TEXTTOKEN, "AB"), t);
        }
    }

    @Test
    public void lexerQuotedText4() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("\"\\\"\"")) {
            Token t = l.next(r);
            Assert.assertEquals(new TextToken(TokenType.TEXTTOKEN, "\""), t);
        }
    }

    @Test
    public void lexerQuotedText5() throws IOException {
        Lexer l = new Lexer();
        try (StringReader r = new StringReader("\"")) {
            try {
                Token t = l.next(r);
            } catch (Lexer.LexerException e) {
                Assert.assertEquals("Unexpected EOF", e.getMessage());
                return ;
            }
        }
        Assert.fail();
    }
}
