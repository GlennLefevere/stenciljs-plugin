package com.github.glennlefevere.stenciljswebcomponents.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AngularProjectListener {
    private static final Logger log = Logger.getInstance(AngularProjectListener.class);
    public static final AngularProjectListener INSTANCE = new AngularProjectListener();

    private final Map<Project, Boolean> angularProject = new HashMap<>();

    public void projectOpened(@NotNull Project project) {
        boolean isAngularApplication = false;

        try {
            Optional<Path> packageJsonPath = getPackageJson(project.getBasePath());
            if (packageJsonPath.isPresent()) {
                String fileContents = String.join("", Files.readAllLines(packageJsonPath.get()));
                isAngularApplication = fileContents.contains("@angular/core");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.angularProject.put(project, isAngularApplication);
    }

    public void projectClosed(@NotNull Project project) {
        this.angularProject.remove(project);
    }

    public Boolean isAngularProject(Project project) {
        return this.angularProject.get(project);
    }

    private Optional<Path> getPackageJson(String projectBasePath) throws IOException {
        Path path = Paths.get(projectBasePath);
        return Files.find(path, 1, (p, b) -> p.getFileName().toString().equalsIgnoreCase("package.json"))
                .findAny();
    }

}
