package com.github.glennlefevere.stenciljswebcomponents.startup;

import com.github.glennlefevere.stenciljswebcomponents.services.AngularProjectService;
import com.github.glennlefevere.stenciljswebcomponents.services.StencilDocService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

public class StartupListener implements ProjectActivity {

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        StencilDocService.getInstance(project).refresh();
        AngularProjectService.getInstance(project).refresh();
        return Unit.INSTANCE;
    }

}
