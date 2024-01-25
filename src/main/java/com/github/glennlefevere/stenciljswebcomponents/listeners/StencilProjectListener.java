package com.github.glennlefevere.stenciljswebcomponents.listeners;

import com.github.glennlefevere.stenciljswebcomponents.model.StencilDoc;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.github.glennlefevere.stenciljswebcomponents.util.ModulePathUtil;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StencilProjectListener {

    private static final Logger log = Logger.getInstance(StencilProjectListener.class);

    public static final StencilProjectListener INSTANCE = new StencilProjectListener();

    private final Map<Project, StencilMergedDoc> projectStencilDocMap = new HashMap<>();
    private final Map<Project, List<Path>> projectStencilDocPathMap = new HashMap<>();

    public void projectOpened(@NotNull Project project) {
        try {
            List<Path> paths = getAllStencilDocs(project.getBasePath());
            StencilMergedDoc mergedDoc = getMergedDocForFilePaths(paths);
            this.projectStencilDocPathMap.put(project, paths);
            this.projectStencilDocMap.put(project, mergedDoc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void projectClosed(@NotNull Project project) {
        this.projectStencilDocMap.remove(project);
        this.projectStencilDocPathMap.remove(project);
    }

    public StencilMergedDoc getStencilMergedDocForProject(Project project) {
        return this.projectStencilDocMap.get(project);
    }

    public boolean isStencilDocFileFromProject(String path) {
        for (Project project : projectStencilDocPathMap.keySet()) {
            return projectStencilDocPathMap.get(project)
                    .stream()
                    .anyMatch(filePath -> filePath.toString()
                            .replaceAll("\\\\", "/")
                            .equals(path));
        }
        return false;
    }

    public Project getProjectFromFilePath(String path) {
        for (Project project : projectStencilDocPathMap.keySet()) {
            if (projectStencilDocPathMap.get(project)
                    .stream()
                    .anyMatch(
                            filePath -> filePath.toString().replaceAll("\\\\", "/").equals(path))) {
                return project;
            }
        }
        return null;
    }

    public void projectFileUpdated(Project project) {
        try {
            StencilMergedDoc mergedDoc = getMergedDocForFilePaths(this.projectStencilDocPathMap.get(project));
            projectStencilDocMap.replace(project, mergedDoc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private StencilMergedDoc getMergedDocForFilePaths(List<Path> paths) throws IOException {
        StencilMergedDoc mergedDoc = new StencilMergedDoc();

        for (Path docPath : paths) {
            String json = String.join("", Files.readAllLines(docPath));
            StencilDoc stencilDoc = new Gson().fromJson(json, StencilDoc.class);
            if (stencilDoc != null && stencilDoc.components != null) {
                mergedDoc.addComponents(stencilDoc.components);
            }
        }

        return mergedDoc;
    }

    private List<Path> getAllStencilDocs(String projectBasePath) throws IOException {
        Set<Path> allStencilModules = getAllModulesUsingStencil(projectBasePath);
        List<Path> allStencilDocs = new ArrayList<>();

        assert allStencilModules != null;
        for (Path stencilModule : allStencilModules) {
            try (Stream<Path> files = Files.walk(stencilModule.getParent())) {
                allStencilDocs.addAll(files
                        .filter(ModulePathUtil::isJsonFile)
                        .filter(ModulePathUtil::isStencilDocsFile)
                        .toList()
                );
            } catch (Exception e) {
                log.error(e);
            }
        }

        return allStencilDocs;
    }

    private Set<Path> getAllModulesUsingStencil(String projectBasePath) throws IOException {
        Path path = Paths.get(projectBasePath);
        Set<Path> result = new HashSet<Path>();

        try (Stream<Path> files = Files.walk(path)) {
            result = files.filter(Files::isRegularFile)
                    .filter(ModulePathUtil::isPackageJsonOfModule)
                    .filter(ModulePathUtil::isStencilModule)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error(e);
        }
        return result;
    }

}
