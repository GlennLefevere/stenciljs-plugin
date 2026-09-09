package com.github.glennlefevere.stenciljswebcomponents.util;

import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ModulePathUtil {

    private ModulePathUtil() {
    }

    public static boolean isPackageJsonOfModule(VirtualFile file) {
        return file != null && !file.isDirectory() &&
               file.getName().equalsIgnoreCase("package.json") &&
               isInDependencyDirectory(file);
    }

    public static boolean isJsonFile(VirtualFile file) {
        return file != null && !file.isDirectory() &&
               file.getName().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    public static boolean isInDependencyDirectory(VirtualFile file) {
        return hasPathSegment(file, "node_modules") || hasPathSegment(file, "dist");
    }

    public static boolean isStencilModule(VirtualFile file) {
        return containsText(file, "@stencil/core");
    }

    public static boolean isStencilDocsFile(VirtualFile file) {
        return containsText(file, "@stencil/core", "\"components\"");
    }

    public static boolean containsText(VirtualFile file, String... needles) {
        return containsAll(file, needles);
    }

    public static Reader openUtf8Reader(VirtualFile file) throws IOException {
        return new BufferedReader(new InputStreamReader(
                new BufferedInputStream(file.getInputStream()), StandardCharsets.UTF_8));
    }

    private static boolean containsAll(VirtualFile file, String... needles) {
        if (file == null || file.isDirectory()) {
            return false;
        }

        boolean[] found = new boolean[needles.length];
        int remaining = needles.length;
        int maxNeedleLength = 0;
        for (String needle : needles) {
            maxNeedleLength = Math.max(maxNeedleLength, needle.length());
        }

        try {
            StringBuilder window = new StringBuilder(maxNeedleLength * 2);
            char[] buffer = new char[8192];
            try (Reader reader = openUtf8Reader(file)) {
                int read;
                while (remaining > 0 && (read = reader.read(buffer)) != -1) {
                    window.append(buffer, 0, read);
                    for (int i = 0; i < needles.length; i++) {
                        if (!found[i] && window.indexOf(needles[i]) >= 0) {
                            found[i] = true;
                            remaining--;
                        }
                    }
                    if (window.length() > maxNeedleLength) {
                        window.delete(0, window.length() - maxNeedleLength);
                    }
                }
            }
            return remaining == 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean hasPathSegment(VirtualFile file, String expected) {
        if (file == null) {
            return false;
        }

        String path = file.getPath().replace('\\', '/');
        for (String segment : path.split("/")) {
            if (segment.equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

}
