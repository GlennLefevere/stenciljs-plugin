package com.github.glennlefevere.stenciljswebcomponents;

import com.github.glennlefevere.stenciljswebcomponents.descriptors.ExtendedHtmlElementDescriptorImpl;
import com.github.glennlefevere.stenciljswebcomponents.listeners.StencilProjectListener;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl;
import com.intellij.psi.impl.source.xml.XmlElementDescriptorProvider;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class WebComponentDescriptorsProvider implements XmlElementDescriptorProvider {
    private static final Logger log = Logger.getInstance(WebComponentDescriptorsProvider.class);

    @Override
    public @Nullable XmlElementDescriptor getDescriptor(XmlTag tag) {
        if (!(tag instanceof HtmlTag)) return null;

        final XmlNSDescriptor nsDescriptor = tag.getNSDescriptor(tag.getNamespace(), false);
        final XmlElementDescriptor descriptor = nsDescriptor != null ? nsDescriptor.getElementDescriptor(tag) : null;

        StencilMergedDoc mergedDoc = StencilProjectListener.INSTANCE.getStencilMergedDocForProject(tag.getProject());

        if (descriptor != null && !(descriptor instanceof AnyXmlElementDescriptor)) {
            if(descriptor instanceof HtmlElementDescriptorImpl htmlElementDescriptor) {
                if(mergedDoc != null && !CollectionUtils.isEmpty(mergedDoc.getComponents())) {
                    try {
                        Field field = htmlElementDescriptor.getClass().getDeclaredField("myRelaxed");
                        field.setAccessible(true);
                        return new ExtendedHtmlElementDescriptorImpl(descriptor, field.getBoolean(htmlElementDescriptor), htmlElementDescriptor.isCaseSensitive(), mergedDoc);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return null;
        }


        if(mergedDoc == null || CollectionUtils.isEmpty(mergedDoc.getComponents()) || mergedDoc.getComponents().stream().noneMatch(comp -> comp.getTag().equals(tag.getName()))) {
            return null;
        }

        return new StencilTagDescriptor(tag, mergedDoc);
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
