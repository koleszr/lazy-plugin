package lusta.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

public class SimpleFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

  private final List<SimpleClassFile> classFiles;

  public SimpleFileManager(StandardJavaFileManager fileManager) {
    super(fileManager);
    this.classFiles = new ArrayList<>();
  }

  @Override
  public JavaFileObject getJavaFileForOutput(
      final Location location, final String className, final Kind kind, final FileObject sibling) {
    final var classFile = new SimpleClassFile(URI.create(String.format("string://%s", className)));
    classFiles.add(classFile);
    return classFile;
  }

  public List<SimpleClassFile> getClassFiles() {
    return List.copyOf(classFiles);
  }
}
