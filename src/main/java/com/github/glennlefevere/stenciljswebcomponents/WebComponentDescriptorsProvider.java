package com.github.glennlefevere.stenciljswebcomponents;

import com.github.glennlefevere.stenciljswebcomponents.descriptors.ExtendedHtmlElementDescriptorImpl;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.github.glennlefevere.stenciljswebcomponents.services.StencilDocService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl;
import com.intellij.psi.impl.source.xml.XmlElementDescriptorProvider;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebComponentDescriptorsProvider implements XmlElementDescriptorProvider {
    private static final Logger LOG = Logger.getInstance(WebComponentDescriptorsProvider.class);
    private static final AtomicBoolean RELAXED_FIELD_WARNING_LOGGED = new AtomicBoolean();

    @Override
    public @Nullable XmlElementDescriptor getDescriptor(XmlTag tag) {
        if (!(tag instanceof HtmlTag)) return null;

        final XmlNSDescriptor nsDescriptor = tag.getNSDescriptor(tag.getNamespace(), false);
        final XmlElementDescriptor descriptor = nsDescriptor != null ? nsDescriptor.getElementDescriptor(tag) : null;

        StencilMergedDoc mergedDoc = StencilDocService.getInstance(tag.getProject()).getMergedDoc();

        if (!(descriptor instanceof AnyXmlElementDescriptor)) {
            if (descriptor instanceof HtmlElementDescriptorImpl htmlElementDescriptor) {
                if (hasComponents(mergedDoc)) {
                    return createExtendedDescriptor(descriptor, htmlElementDescriptor, mergedDoc);
                }
            }
            if (descriptor != null) {
                return null;
            }
        }


        if (!hasComponents(mergedDoc) || mergedDoc.getComponents().stream().noneMatch(comp -> comp.getTag().equals(tag.getName()))) {
            return null;
        }

        return new StencilTagDescriptor(tag, mergedDoc);
    }

    private static boolean hasComponents(@Nullable StencilMergedDoc mergedDoc) {
        return mergedDoc != null && mergedDoc.getComponents() != null && !mergedDoc.getComponents().isEmpty();
    }

    private static @Nullable XmlElementDescriptor createExtendedDescriptor(
            XmlElementDescriptor descriptor,
            HtmlElementDescriptorImpl htmlElementDescriptor,
            StencilMergedDoc mergedDoc) {
        try {
            Field field = HtmlElementDescriptorImpl.class.getDeclaredField("myRelaxed");
            if (!field.trySetAccessible()) {
                logRelaxedFieldWarning(htmlElementDescriptor, null);
                return null;
            }
            return new ExtendedHtmlElementDescriptorImpl(
                    descriptor,
                    field.getBoolean(htmlElementDescriptor),
                    htmlElementDescriptor.isCaseSensitive(),
                    mergedDoc);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException exception) {
            logRelaxedFieldWarning(htmlElementDescriptor, exception);
            return null;
        }
    }

    private static void logRelaxedFieldWarning(
            HtmlElementDescriptorImpl htmlElementDescriptor,
            @Nullable Exception exception) {
        if (!RELAXED_FIELD_WARNING_LOGGED.compareAndSet(false, true)) {
            return;
        }

        String message = "Unable to read HtmlElementDescriptorImpl.myRelaxed for " +
                htmlElementDescriptor.getClass().getName() +
                "; using IntelliJ's default HTML descriptor";
        if (exception == null) {
            LOG.warn(message);
        } else {
            LOG.warn(message, exception);
        }
    }

    private static XmlElementDescriptor getWrappedDescriptorFromNamespace(@NotNull XmlTag xmlTag) {
        XmlElementDescriptor elementDescriptor = null;
        final XmlNSDescriptor nsDescriptor = xmlTag.getNSDescriptor(xmlTag.getNamespace(), false);

        if (nsDescriptor != null) {
            if (!DumbService.getInstance(xmlTag.getProject()).isDumb() || DumbService.isDumbAware(nsDescriptor)) {
                elementDescriptor = nsDescriptor.getElementDescriptor(xmlTag);
            }
        }
        if (elementDescriptor instanceof HtmlElementDescriptorImpl) {
            return new StencilStandardTagDescriptor((HtmlElementDescriptorImpl)elementDescriptor);
        }
        return null;
    }
}
