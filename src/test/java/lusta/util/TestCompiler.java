package lusta.util;

import java.io.StringWriter;
import java.util.List;
import javax.tools.ToolProvider;
import lusta.LazyPlugin;

public class TestCompiler {

  public byte[] compile(final String qualifiedClassName, final String testSource) {
    final var output = new StringWriter();
    final var compiler = ToolProvider.getSystemJavaCompiler();
    final var fileManager =
        new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
    final var compilationUnits = List.of(new SimpleSourceFile(qualifiedClassName, testSource));

    final var arguments =
        List.of(
            "-classpath",
            System.getProperty("java.class.path"),
            "-Xlint:unchecked",
            String.format("-Xplugin:%s", LazyPlugin.NAME));

    final var compilationTask =
        compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);
    compilationTask.call();

    return fileManager.getClassFiles().iterator().next().getCompiledBinaries();
  }
}
