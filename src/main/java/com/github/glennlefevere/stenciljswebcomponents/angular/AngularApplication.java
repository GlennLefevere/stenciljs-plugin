package com.github.glennlefevere.stenciljswebcomponents.angular;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class AngularApplication {
    private static final Logger log = Logger.getInstance(AngularApplication.class);

    public static AngularApplication INSTANCE = new AngularApplication();

    public boolean isAngularApplication = false;

    public AngularApplication() {
        init();
    }

    private void init() {
        for (Project project : Arrays.stream(ProjectManager.getInstance().getOpenProjects()).filter(project -> project.getBasePath() != null).toList()) {
            try {
                Optional<Path> packageJsonPath = getPackageJson(project.getBasePath());
                if (packageJsonPath.isPresent()) {
                    String fileContents = String.join("", Files.readAllLines(packageJsonPath.get()));
                    this.isAngularApplication = fileContents.contains("@angular/core");
                    break;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Optional<Path> getPackageJson(String projectBasePath) throws IOException {
        Path path = Paths.get(projectBasePath);
        Optional<Path> pathOptional = Optional.empty();

        try (Stream<Path> files = Files.walk(path)) {
            pathOptional = files.filter(Files::isRegularFile)
                    .filter(file -> !file.toAbsolutePath().toString().toLowerCase().contains("node_modules"))
                    .filter(file -> file.toAbsolutePath().toString().toLowerCase().endsWith("package.json"))
                    .findAny();
        } catch (Exception e) {
            log.error(e);
        }
        return pathOptional;
    }

}
