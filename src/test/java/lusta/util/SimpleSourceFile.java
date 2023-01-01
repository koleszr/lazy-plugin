package lusta.util;

import java.io.IOException;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;

public class SimpleSourceFile extends SimpleJavaFileObject {

  private final String content;

  public SimpleSourceFile(final String qualifiedClassName, final String content) {
    super(
        URI.create(
            String.format(
                "file://%s%s", qualifiedClassName.replaceAll("\\.", "/"), Kind.SOURCE.extension)),
        Kind.SOURCE);
    this.content = content;
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
    return content;
  }
}
