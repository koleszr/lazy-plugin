package lusta.util;

import java.lang.reflect.InvocationTargetException;

public class TestRunner {

  public Object run(
      final byte[] byteCode, final String qualifiedClassName, final String methodName) {
    final var classLoader =
        new ClassLoader() {
          @Override
          protected Class<?> findClass(String name) throws ClassNotFoundException {
            return defineClass(name, byteCode, 0, byteCode.length);
          }
        };

    try {
      final var clazz = classLoader.loadClass(qualifiedClassName);
      final var method = clazz.getMethod(methodName);
      return method.invoke(null);
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException("Cannot load compiled test class!", e);
    } catch (final NoSuchMethodException e) {
      throw new RuntimeException(
          String.format("Cannot find %s method in the compiled test class!", methodName), e);
    } catch (final InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
