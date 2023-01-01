package lusta;

import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import java.util.Set;
import java.util.function.Supplier;
import javax.lang.model.type.TypeKind;

/**
 * javac -cp target/classes/ -d target/classes/ \
 * --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
 * --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
 * --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
 * --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED src/main/java/lusta/*.java
 *
 * <p>javac -cp target/classes/ -d target/classes/ -Xplugin:LazyPlugin \
 * -J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
 * -J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
 * -J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
 * -J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED src/main/java/App.java
 */
public class LazyPlugin implements Plugin {

  public static final String NAME = LazyPlugin.class.getSimpleName();

  public String getName() {
    return NAME;
  }

  public void init(final JavacTask task, final String... args) {
    final var context = ((BasicJavacTask) task).getContext();

    task.addTaskListener(
        new TaskListener() {
          @Override
          public void finished(final TaskEvent event) {
            if (event.getKind() != Kind.PARSE) {
              return;
            }

            event.getCompilationUnit().accept(new VariableVisitor(context), null);

            TaskListener.super.finished(event);
          }
        });
  }

  private static class VariableVisitor extends TreeScanner<Void, Void> {

    private static final Set<Class<? extends JCExpression>> ACCEPTED_TYPES =
        Set.of(JCLiteral.class, JCNewClass.class);

    private final TreeMaker factory;

    private final Names namesTable;

    VariableVisitor(final Context context) {
      this.factory = TreeMaker.instance(context);
      this.namesTable = Names.instance(context);
    }

    @Override
    public Void visitVariable(final VariableTree node, final Void unused) {
      final var initializer = node.getInitializer();

      if (isLazy(node) && initializer != null && node instanceof JCVariableDecl variableNode) {
        setVariableType(variableNode);
        setInit(variableNode);
      }

      return super.visitVariable(node, unused);
    }

    private boolean isLazy(final VariableTree node) {
      return node.getModifiers().getAnnotations().stream()
          .anyMatch(
              annotation ->
                  lazy.class.getSimpleName().equals(annotation.getAnnotationType().toString()));
    }

    private void setVariableType(final JCVariableDecl variableNode) {
      final var variableType = variableNode.vartype;
      if (variableType == null && variableNode.declaredUsingVar()) {
        return;
      }

      final var init = variableNode.init;
      if (variableType == null && init != null) {
        variableNode.vartype = resolveVariableTypeFromInit(init);
        return;
      }

      if (variableType != null) {
        final var javaUtilFunctionSupplier = resolveType(Supplier.class);
        final var convertedVarType = convertVariableTypeIfPrimitive(variableType);
        final var typeArguments =
            convertedVarType == null ? List.<JCExpression>nil() : List.of(convertedVarType);
        variableNode.vartype = factory.TypeApply(javaUtilFunctionSupplier, typeArguments);
        return;
      }

      throw new IllegalArgumentException("Unexpected stuff");
    }

    private JCExpression resolveVariableTypeFromInit(final JCExpression init) {
      final JCExpression typeVariable;

      if (init instanceof JCLiteral literal) {
        typeVariable = resolveType(literal.value.getClass());
      } else if (init instanceof JCNewClass newClass) {
        typeVariable = newClass.clazz;
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Expected on of %s, got %s!", ACCEPTED_TYPES, init.getClass().getSimpleName()));
      }

      final var javaUtilFunctionSupplier = resolveType(Supplier.class);
      return factory.TypeApply(
          javaUtilFunctionSupplier, typeVariable == null ? List.nil() : List.of(typeVariable));
    }

    private void setInit(final JCVariableDecl variableNode) {
      final var typeArguments =
          variableNode.vartype == null
              ? List.<JCExpression>nil()
              : getTypeArguments(((JCTypeApply) variableNode.vartype));

      final var fieldAccess = resolveType(LazySupplier.class);
      final var clazz = factory.TypeApply(fieldAccess, typeArguments);
      variableNode.init =
          factory.NewClass(
              null,
              typeArguments,
              clazz,
              List.of(factory.Lambda(List.nil(), variableNode.init)),
              null);
    }

    private List<JCExpression> getTypeArguments(final JCTypeApply typeApply) {
      final var typeArguments = typeApply.arguments;
      if (typeArguments.length() > 1) {
        throw new IllegalArgumentException("Expected at most 1 type argument!");
      }

      final var converted = convertVariableTypeIfPrimitive(typeArguments.head);
      return converted == null ? List.nil() : List.of(converted);
    }

    private JCExpression convertVariableTypeIfPrimitive(final JCExpression tree) {
      if (tree instanceof JCPrimitiveTypeTree primitiveType) {
        return convertPrimitiveTypeToBoxed(primitiveType.getPrimitiveTypeKind());
      }

      return tree;
    }

    private JCExpression convertPrimitiveTypeToBoxed(final TypeKind primitiveTypeKind) {
      final var boxedName =
          switch (primitiveTypeKind) {
            case BOOLEAN -> "Boolean";
            case BYTE -> "Byte";
            case SHORT -> "Short";
            case INT -> "Integer";
            case LONG -> "Long";
            case CHAR -> "Char";
            case FLOAT -> "Float";
            case DOUBLE -> "Double";
            default -> throw new IllegalArgumentException(
                String.format("Expected primitive type, got %s", primitiveTypeKind));
          };
      return factory.Ident(namesTable.fromString(boxedName));
    }

    private JCExpression resolveType(final Class<?> clazz) {
      final var packageName = clazz.getPackageName();
      final var packageNameParts = packageName.split("\\.");

      final var classIdentifier = namesTable.fromString(clazz.getSimpleName());
      if (packageNameParts.length == 0) {
        return factory.Ident(classIdentifier);
      }

      return factory.Select(
          resolveTypeRecursively(packageNameParts, packageNameParts.length - 1), classIdentifier);
    }

    private JCExpression resolveTypeRecursively(
        final String[] packageNameParts, final int currentIndex) {
      if (currentIndex == 0) {
        return factory.Ident(namesTable.fromString(packageNameParts[0]));
      }

      return factory.Select(
          resolveTypeRecursively(packageNameParts, currentIndex - 1),
          namesTable.fromString(packageNameParts[currentIndex]));
    }
  }
}
