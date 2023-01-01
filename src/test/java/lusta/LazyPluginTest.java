package lusta;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lusta.util.TestCompiler;
import lusta.util.TestRunner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// @Disabled
public class LazyPluginTest {

  private static final String IMPLICITLY_TYPED_METHOD_NAME = "testImplicitlyTyped";
  private static final String EXPLICITLY_TYPED_METHOD_NAME = "testExplicitlyTyped";

  private static final String STRING_LITERAL_SOURCE =
      """
          package lusta;

          public class LazyStringLiteralTest {
            public static String testImplicitlyTyped() {
              @lazy final var lazyString = "hello";
              return lazyString.get();
            }

            public static String testExplicitlyTyped() {
              @lazy final String lazyString = "hello";
              return lazyString.get();
            }
          }""";

  private static final String INTEGER_LITERAL_SOURCE =
      """
          package lusta;

          public class LazyIntegerLiteralTest {
            public static Integer testImplicitlyTyped() {
              @lazy final var lazyInt = 42;
              return lazyInt.get();
            }

            public static Integer testExplicitlyTyped() {
              @lazy final int lazyInt = 42;
              return lazyInt.get();
            }
          }""";

  private static final String PRIMITIVE_ARRAY_SOURCE =
      """
          package lusta;

          public class LazyIntArrayTest {
            public static int[] testImplicitlyTyped() {
              @lazy final var lazyIntArray = new int[]{ 1, 2, 3 };
              return lazyIntArray.get();
            }

            public static int[] testExplicitlyTyped() {
              @lazy final int[] lazyIntArray = new int[]{ 1, 2, 3 };
              return lazyIntArray.get();
            }
          }""";

  private static final String NEW_CLASS_SOURCE =
      """
          package lusta;

          import lusta.LazyPluginTest.TestRecord;

          public class LazyNewClassTest {
            public static TestRecord testImplicitlyTyped() {
              @lazy final var lazyTestRecord = new TestRecord(42, "hello");
              return lazyTestRecord.get();
            }

            public static TestRecord testExplicitlyTyped() {
              @lazy final TestRecord lazyTestRecord = new TestRecord(42, "hello");
              return lazyTestRecord.get();
            }
          }""";

  private static final String METHOD_INVOCATION_SOURCE =
      """
          package lusta;

          import static lusta.LazyPluginTest.hello;

          public class LazyMethodInvocationTest {
            public static String testImplicitlyTyped() {
              @lazy final var lazyMethodInvocation = hello();
              return lazyMethodInvocation.get();
            }

            public static String testExplicitlyTyped() {
              @lazy final String lazyMethodInvocation = hello();
              return lazyMethodInvocation.get();
            }
          }""";

  private final TestCompiler compiler = new TestCompiler();

  private final TestRunner runner = new TestRunner();

  @ParameterizedTest
  @ValueSource(strings = {IMPLICITLY_TYPED_METHOD_NAME, EXPLICITLY_TYPED_METHOD_NAME})
  void compileLazyStringLiteral(final String methodName) {
    final var actual =
        compileAndRun(
            "lusta.LazyStringLiteralTest", STRING_LITERAL_SOURCE, methodName, String.class);
    final var expected = "hello";
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {IMPLICITLY_TYPED_METHOD_NAME, EXPLICITLY_TYPED_METHOD_NAME})
  void compileLazyIntegerLiteral(final String methodName) {
    final var actual =
        compileAndRun(
            "lusta.LazyIntegerLiteralTest", INTEGER_LITERAL_SOURCE, methodName, Integer.class);
    final var expected = 42;
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {IMPLICITLY_TYPED_METHOD_NAME, EXPLICITLY_TYPED_METHOD_NAME})
  void compileLazyIntArray(final String methodName) {
    final var actual =
        compileAndRun("lusta.LazyIntArrayTest", PRIMITIVE_ARRAY_SOURCE, methodName, int[].class);
    final var expected = new int[] {1, 2, 3};
    assertArrayEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {IMPLICITLY_TYPED_METHOD_NAME, EXPLICITLY_TYPED_METHOD_NAME})
  void compileLazyNewClass(final String methodName) {
    final var actual =
        compileAndRun("lusta.LazyNewClassTest", NEW_CLASS_SOURCE, methodName, TestRecord.class);
    final var expected = new TestRecord(42, "hello");
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {IMPLICITLY_TYPED_METHOD_NAME, EXPLICITLY_TYPED_METHOD_NAME})
  void compileLazyMethodInvocation(final String methodName) {
    final var actual =
        compileAndRun(
            "lusta.LazyMethodInvocationTest", METHOD_INVOCATION_SOURCE, methodName, String.class);
    final var expected = "hello";
    assertEquals(expected, actual);
  }

  private <T> T compileAndRun(
      final String qualifiedClassName,
      final String source,
      final String methodName,
      final Class<T> expectedType) {
    final var byteCode = compiler.compile(qualifiedClassName, source);

    final var actual = runner.run(byteCode, qualifiedClassName, methodName);
    assertEquals(expectedType, actual.getClass());

    @SuppressWarnings("unchecked")
    final var t = (T) actual;
    return t;
  }

  public record TestRecord(int i, String s) {}

  public static String hello() {
    return "hello";
  }
}
