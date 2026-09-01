package dev.mintychochip.editor;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Result of an import operation.
 *
 * @param tasksImported number of tasks successfully imported
 * @param tasksDeleted number of tasks deleted during import
 * @param errors list of error messages encountered during import
 */
public record ImportResult(int tasksImported, int tasksDeleted, @NotNull List<String> errors) {}
