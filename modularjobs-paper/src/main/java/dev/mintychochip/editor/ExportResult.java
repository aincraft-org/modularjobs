package dev.mintychochip.editor;

import org.jetbrains.annotations.NotNull;

/**
 * Result of a REST editor export operation.
 *
 * @param sessionCode public session code used by {@code /jobs applyedits}
 * @param webEditorUrl full URL to the web editor with the session loaded
 * @param sessionToken secret token delivered in the URL fragment
 */
public record ExportResult(
    @NotNull String sessionCode, @NotNull String webEditorUrl, @NotNull String sessionToken) {}
