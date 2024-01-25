package com.github.glennlefevere.stenciljswebcomponents.startup;

import com.github.glennlefevere.stenciljswebcomponents.listeners.AngularProjectListener;
import com.github.glennlefevere.stenciljswebcomponents.listeners.StencilProjectListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class StartupListener implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        StencilProjectListener.INSTANCE.projectOpened(project);
        AngularProjectListener.INSTANCE.projectOpened(project);
    }

}
