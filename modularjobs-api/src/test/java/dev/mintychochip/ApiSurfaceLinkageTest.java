package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApiSurfaceLinkageTest {

  private static final List<String> FORBIDDEN_PUBLIC_NAMESPACES =
      List.of(
          "dev.mintychochip.common.",
          "org.bukkit.",
          "io.papermc.",
          "org.spigotmc.",
          "com.destroystokyo.",
          "net.minecraft.");

  @Test
  void everyExportedSignatureLinksWithoutImplementationModules() throws IOException {
    Path classes = Path.of("build/classes/java/main");
    assertTrue(Files.isDirectory(classes), "compiled api classes must exist");

    List<String> failures = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(classes)) {
      for (String className :
          walk.filter(path -> path.toString().endsWith(".class"))
              .map(classes::relativize)
              .map(Path::toString)
              .map(name -> name.substring(0, name.length() - ".class".length()))
              .map(name -> name.replace(File.separatorChar, '.'))
              .sorted()
              .toList()) {
        inspectPublicClass(className, failures);
      }
    }

    assertTrue(
        failures.isEmpty(),
        () -> "Unlinkable or implementation-dependent public API signatures: " + failures);
  }

  private static void inspectPublicClass(String className, List<String> failures) {
    try {
      Class<?> type = Class.forName(className, false, ApiSurfaceLinkageTest.class.getClassLoader());
      if (!Modifier.isPublic(type.getModifiers())) {
        return;
      }

      inspectType(type.getName(), type, failures);
      inspectType(type.getName(), type.getGenericSuperclass(), failures);
      inspectTypes(type.getName(), type.getGenericInterfaces(), failures);
      inspectTypeVariables(type.getName(), type.getTypeParameters(), failures);
      inspectTypes(type.getName(), type.getPermittedSubclasses(), failures);

      for (Field field : type.getDeclaredFields()) {
        if (isExported(field.getModifiers())) {
          inspectType(type.getName() + "#" + field.getName(), field.getGenericType(), failures);
        }
      }
      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
        if (isExported(constructor.getModifiers())) {
          String owner = type.getName() + " constructor";
          inspectTypes(owner, constructor.getGenericParameterTypes(), failures);
          inspectTypes(owner, constructor.getGenericExceptionTypes(), failures);
          inspectTypeVariables(owner, constructor.getTypeParameters(), failures);
        }
      }
      for (Method method : type.getDeclaredMethods()) {
        if (isExported(method.getModifiers())) {
          String owner = type.getName() + "#" + method.getName();
          inspectType(owner, method.getGenericReturnType(), failures);
          inspectTypes(owner, method.getGenericParameterTypes(), failures);
          inspectTypes(owner, method.getGenericExceptionTypes(), failures);
          inspectTypeVariables(owner, method.getTypeParameters(), failures);
        }
      }
      for (RecordComponent component :
          type.isRecord() ? type.getRecordComponents() : new RecordComponent[0]) {
        inspectType(
            type.getName() + "#" + component.getName(), component.getGenericType(), failures);
      }
    } catch (ClassNotFoundException
        | LinkageError
        | TypeNotPresentException
        | MalformedParameterizedTypeException exception) {
      failures.add(className + " failed to link: " + exception);
    }
  }

  private static boolean isExported(int modifiers) {
    return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
  }

  private static void inspectTypeVariables(
      String owner, TypeVariable<?>[] variables, List<String> failures) {
    for (TypeVariable<?> variable : variables) {
      inspectTypes(owner, variable.getBounds(), failures);
    }
  }

  private static void inspectTypes(String owner, Type[] types, List<String> failures) {
    if (types == null) {
      return;
    }
    for (Type type : types) {
      inspectType(owner, type, failures);
    }
  }

  private static void inspectType(String owner, Type type, List<String> failures) {
    if (type == null) {
      return;
    }
    String typeName = type.getTypeName();
    for (String forbiddenNamespace : FORBIDDEN_PUBLIC_NAMESPACES) {
      if (typeName.contains(forbiddenNamespace)) {
        failures.add(owner + " references " + typeName);
      }
    }
  }
}
