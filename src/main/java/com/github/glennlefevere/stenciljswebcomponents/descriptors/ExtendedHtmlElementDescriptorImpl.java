package com.github.glennlefevere.stenciljswebcomponents.descriptors;

import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;

import java.util.Objects;

public class ExtendedHtmlElementDescriptorImpl extends HtmlElementDescriptorImpl {
    private static final Logger log = Logger.getInstance(ExtendedHtmlElementDescriptorImpl.class);

    private final StencilMergedDoc mergedDoc;

    public ExtendedHtmlElementDescriptorImpl(XmlElementDescriptor _delegate, boolean relaxed, boolean caseSensitive, StencilMergedDoc mergedDoc) {
        super(_delegate, relaxed, caseSensitive);
        this.mergedDoc = mergedDoc;
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(String attributeName, XmlTag context) {
        XmlAttributeDescriptor descriptor = super.getAttributeDescriptor(attributeName, context);

        if(!Objects.equals(attributeName, "slot") || descriptor == null) {
            return descriptor;
        }

        return new ExtendedHtmlAttributeDescriptorImpl(descriptor, this.isCaseSensitive(), context, this.mergedDoc);
    }

    @Override
    public XmlAttributeDescriptor getDefaultAttributeDescriptor(String attributeName, XmlTag context) {
        XmlAttributeDescriptor descriptor = super.getDefaultAttributeDescriptor(attributeName, context);

        if(!Objects.equals(attributeName, "slot") || descriptor == null) {
            return descriptor;
        }

        return new ExtendedHtmlAttributeDescriptorImpl(descriptor, this.isCaseSensitive(), context, this.mergedDoc);
    }
}
