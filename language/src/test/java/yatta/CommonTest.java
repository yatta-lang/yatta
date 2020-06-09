package yatta;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import yatta.ast.expression.PatternLetNode;

import java.io.IOException;

public abstract class CommonTest {
  protected Context context;

  @BeforeAll
  public void initEngine() {
    context = Context.newBuilder().allowAllAccess(true).environment("YATTA_STDLIB_HOME", "lib-yatta").build();
  }

  @AfterAll
  public void dispose() {
    try {
      context.eval(Source.newBuilder(YattaLanguage.ID, "shutdown", "shutdown").internal(true).build());
    } catch (IOException e) {
    }
    context.close();
  }

  /**
   * This can be used in Context Builder using .option(, "<log level>")
   */
  @SuppressWarnings("unused")
  static String logLevelOption(Class<?> cls) {
    return "log." + YattaLanguage.ID + "." + cls.getName() + ".level";
  }
}
