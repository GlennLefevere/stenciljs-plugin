package com.github.glennlefevere.stenciljswebcomponents.services;

import com.github.glennlefevere.stenciljswebcomponents.model.StencilDoc;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.github.glennlefevere.stenciljswebcomponents.util.ModulePathUtil;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.psi.search.FilenameIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project-scoped cache of the Stencil component documentation available to a project.
 *
 * <p>The expensive part of discovering modules is deliberately scheduled as a
 * non-blocking read action. Keeping this state on the project service also means it
 * is released when the project closes instead of retaining projects in an application
 * singleton.</p>
 */
@Service(Service.Level.PROJECT)
public final class StencilDocService {

    private static final Logger LOG = Logger.getInstance(StencilDocService.class);
    private static final Gson GSON = new Gson();

    private final Project project;
    private final Map<VirtualFile, CachedDocument> documentCache = new ConcurrentHashMap<>();
    private volatile StencilMergedDoc mergedDoc;

    public StencilDocService(@NotNull Project project) {
        this.project = project;

        project.getMessageBus().connect(project).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull java.util.List<? extends VFileEvent> events) {
                if (events.stream()
                        .map(VFileEvent::getFile)
                        .anyMatch(StencilDocService.this::isPotentialStencilFile)) {
                    refresh();
                }
            }
        });
    }

    public static StencilDocService getInstance(@NotNull Project project) {
        return project.getService(StencilDocService.class);
    }

    /**
     * Starts a coalesced refresh. This method only schedules work and is safe to call
     * from startup activities, completion providers, or VFS notifications.
     */
    public void refresh() {
        if (project.isDisposed()) {
            return;
        }

        ReadAction.nonBlocking(this::buildSnapshot)
                .inSmartMode(project)
                .coalesceBy(this)
                .expireWith(project)
                .submit(AppExecutorUtil.getAppExecutorService())
                .onSuccess(snapshot -> {
                    if (!project.isDisposed()) {
                        mergedDoc = snapshot;
                    }
                })
                .onError(error -> LOG.warn("Unable to refresh Stencil component documentation", error));
    }

    @Nullable
    public StencilMergedDoc getMergedDoc() {
        return mergedDoc;
    }

    private StencilMergedDoc buildSnapshot() {
        Collection<VirtualFile> packageJsons = findPackageJsons();
        Set<VirtualFile> stencilModules = new LinkedHashSet<>();

        for (VirtualFile packageJson : packageJsons) {
            if (ModulePathUtil.isStencilModule(packageJson)) {
                stencilModules.add(packageJson);
            }
        }

        Set<VirtualFile> docs = new LinkedHashSet<>();
        Set<VirtualFile> visitedDirectories = new HashSet<>();
        for (VirtualFile stencilModule : stencilModules) {
            VirtualFile moduleDirectory = stencilModule.getParent();
            if (moduleDirectory != null) {
                collectStencilDocs(moduleDirectory, docs, visitedDirectories);
            }
        }

        return readMergedDoc(docs);
    }

    private Collection<VirtualFile> findPackageJsons() {
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Collection<VirtualFile> packageJsons = new LinkedHashSet<>(
                FilenameIndex.getVirtualFilesByName("package.json", scope));

        // A dependency directory can be excluded from the index in some project
        // configurations. In that case, inspect only the direct dependency roots as
        // a bounded VFS fallback; never fall back to walking the whole project.
        VirtualFile baseDirectory = ProjectUtil.guessProjectDir(project);
        if (baseDirectory != null) {
            addFallbackPackages(baseDirectory.findChild("node_modules"), packageJsons);
            addFallbackPackages(baseDirectory.findChild("dist"), packageJsons);
        }
        return packageJsons;
    }

    private void addFallbackPackages(@Nullable VirtualFile root, Collection<VirtualFile> packageJsons) {
        if (root == null || packageJsons.stream().anyMatch(file -> isUnder(file, root))) {
            return;
        }
        collectPackageJsons(root, packageJsons, new HashSet<>());
    }

    private void collectPackageJsons(VirtualFile directory,
                                     Collection<VirtualFile> packageJsons,
                                     Set<VirtualFile> visitedDirectories) {
        if (!directory.isValid() || !visitedDirectories.add(directory)) {
            return;
        }

        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                collectPackageJsons(child, packageJsons, visitedDirectories);
            } else if (ModulePathUtil.isPackageJsonOfModule(child)) {
                packageJsons.add(child);
            }
        }
    }

    private void collectStencilDocs(VirtualFile directory,
                                    Set<VirtualFile> docs,
                                    Set<VirtualFile> visitedDirectories) {
        if (!directory.isValid() || !visitedDirectories.add(directory)) {
            return;
        }

        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                if (!child.getName().equalsIgnoreCase("node_modules") &&
                        !child.getName().equalsIgnoreCase(".git")) {
                    collectStencilDocs(child, docs, visitedDirectories);
                }
            } else if (ModulePathUtil.isJsonFile(child) && ModulePathUtil.isStencilDocsFile(child)) {
                docs.add(child);
            }
        }
    }

    private StencilMergedDoc readMergedDoc(Set<VirtualFile> paths) {
        StencilMergedDoc result = new StencilMergedDoc();

        for (VirtualFile path : paths) {
            CachedDocument cached = documentCache.get(path);
            long modificationStamp = path.getModificationStamp();
            if (cached == null || cached.modificationStamp != modificationStamp) {
                cached = new CachedDocument(modificationStamp, parseDocument(path));
                documentCache.put(path, cached);
            }

            if (cached.document != null && cached.document.components != null) {
                result.addComponents(cached.document.components);
            }
        }

        documentCache.keySet().removeIf(path -> !paths.contains(path));

        return result;
    }

    @Nullable
    private StencilDoc parseDocument(VirtualFile path) {
        try (Reader reader = ModulePathUtil.openUtf8Reader(path)) {
            return GSON.fromJson(reader, StencilDoc.class);
        } catch (IOException | JsonParseException e) {
            LOG.warn("Unable to read Stencil documentation from " + path.getPath(), e);
            return null;
        }
    }

    private boolean isPotentialStencilFile(@Nullable VirtualFile file) {
        return ModulePathUtil.isJsonFile(file) &&
                ModulePathUtil.isInDependencyDirectory(file) && isUnderProject(file);
    }

    private boolean isUnderProject(@NotNull VirtualFile file) {
        VirtualFile baseDirectory = ProjectUtil.guessProjectDir(project);
        return baseDirectory != null && isUnder(file, baseDirectory);
    }

    private static boolean isUnder(@NotNull VirtualFile file, @NotNull VirtualFile directory) {
        VirtualFile current = file;
        while (current != null) {
            if (current.equals(directory)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static final class CachedDocument {
        private final long modificationStamp;
        private final StencilDoc document;

        private CachedDocument(long modificationStamp, @Nullable StencilDoc document) {
            this.modificationStamp = modificationStamp;
            this.document = document;
        }
    }
}
