package com.github.glennlefevere.stenciljswebcomponents.services;

import com.github.glennlefevere.stenciljswebcomponents.util.ModulePathUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

/** Project-scoped Angular project detection. */
@Service(Service.Level.PROJECT)
public final class AngularProjectService {

    private static final Logger LOG = Logger.getInstance(AngularProjectService.class);

    private final Project project;
    private volatile boolean angularProject;

    public AngularProjectService(@NotNull Project project) {
        this.project = project;
    }

    public static AngularProjectService getInstance(@NotNull Project project) {
        return project.getService(AngularProjectService.class);
    }

    public void refresh() {
        if (project.isDisposed()) {
            return;
        }

        ReadAction.nonBlocking(this::detectAngularProject)
                .expireWith(project)
                .submit(AppExecutorUtil.getAppExecutorService())
                .onSuccess(result -> angularProject = result)
                .onError(error -> LOG.warn("Unable to detect whether project uses Angular", error));
    }

    public boolean isAngularProject() {
        return angularProject;
    }

    private boolean detectAngularProject() {
        VirtualFile baseDirectory = ProjectUtil.guessProjectDir(project);
        VirtualFile packageJson = baseDirectory == null ? null : baseDirectory.findChild("package.json");
        return ModulePathUtil.containsText(packageJson, "@angular/core");
    }
}
