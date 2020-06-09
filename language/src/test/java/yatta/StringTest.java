package yatta;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StringTest extends CommonTest {
  @Test
  void testSimpleInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "let who = \"world\" in \"hello {who}\"").asString();
    assertEquals("hello world", ret);
  }

  @Test
  void testSimpleInterpolation2() {
    String ret = context.eval(YattaLanguage.ID, "let xyz = 5 in \"number: {xyz}\"").asString();
    assertEquals("number: 5", ret);
  }

  @Test
  void testSimpleInterpolation3() {
    String ret = context.eval(YattaLanguage.ID, "\"hello {\"world\"}\"").asString();
    assertEquals("hello world", ret);
  }

  @Test
  void testAlignmentInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "let var = \"abcde\" in \"{var,10}\"").asString();
    assertEquals("     abcde", ret);
  }

  @Test
  void testAlignmentWithAVarInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "let\n" +
        "    var = \"abcde\"\n" +
        "    align = 10\n" +
        "in \n" +
        "    \"{var,align}\"").asString();
    assertEquals("     abcde", ret);
  }

  @Test
  void testNegativeAlignmentInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "let var = \"abcde\" in \"{var,-10}\"").asString();
    assertEquals("abcde     ", ret);
  }

  @Test
  void testSimplePromiseInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "let\n" +
        "    who = async \\->\"world\"\n" +
        "in\n" +
        "    \"hello {who}\"").asString();
    assertEquals("hello world", ret);
  }

  @Test
  void testAlignmentPromiseInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "let\n" +
        "    var = async \\->\"abcde\"\n" +
        "in\n" +
        "    \"{var,(async \\-> 10)}\"").asString();
    assertEquals("     abcde", ret);
  }

  @Test
  void testNegativeAlignmentPromiseInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "let var = \"abcde\" in \"{(async \\->var),(async \\->-10)}\"").asString();
    assertEquals("abcde     ", ret);
  }

  @Test
  void testMultiplePromiseInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "let var = \"abcde\" in\n" +
        "\"{(async \\->var),(async \\->-10)}:{(async \\->var),(async \\->10)}\"").asString();
    assertEquals("abcde     :     abcde", ret);
  }

  @Test
  void testMultiline() {
    String ret = context.eval(YattaLanguage.ID, "\"hello\nworld\"").asString();
    assertEquals("hello\nworld", ret);
  }

  @Test
  void testUnicodeCharacter() {
    int ret = context.eval(YattaLanguage.ID, "'あ'").asInt();
    assertEquals('あ', ret);
  }

  @Test
  void testEscapeCurlyInterpolation() {
    String ret = context.eval(YattaLanguage.ID, "\"{{hello}}\"").asString();
    assertEquals("{hello}", ret);
  }

  @Test
  void testInterpolationInCurly() {
    String ret = context.eval(YattaLanguage.ID, "let who = \"world\" in \"hello {{{who}}}\"").asString();
    assertEquals("hello {world}", ret);
  }

  @TestFactory
  Stream<DynamicTest> escapeSequencesCurly() {
    String[] escapeSequences = {
        "\\'",
        "\\a",
        "\\b",
        "\\f",
        "\\n",
        "\\r",
        "\\t",
        "\\v"
    };

    return Arrays.stream(escapeSequences).map(sequence -> DynamicTest.dynamicTest(sequence, () -> {
      String ret = context.eval(YattaLanguage.ID, '"' + sequence + '"').asString();
      assertEquals(sequence, ret);
    }));
  }

  @Test
  void testUnicodeLiterals() {
    String ret = context.eval(YattaLanguage.ID, "\"\\u0070\\u0075\\u0062\\u006c\\u0069\\u0063 \\u0063\\u006c\\u0061\\u0073\\u0073\\u0020\\u0054\\u0065\\u0073\\u0074\"").asString();
    assertEquals("public class Test", StringEscapeUtils.unescapeJava(ret));
  }
}
