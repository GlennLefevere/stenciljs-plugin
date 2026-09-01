package com.github.glennlefevere.stenciljswebcomponents.descriptors;

import com.github.glennlefevere.stenciljswebcomponents.model.StencilDocComponent;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.github.glennlefevere.stenciljswebcomponents.util.CompletionTypeUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.impl.source.html.dtd.HtmlAttributeDescriptorImpl;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;

import java.util.List;

public class ExtendedHtmlAttributeDescriptorImpl extends HtmlAttributeDescriptorImpl {
    private static final Logger log = Logger.getInstance(ExtendedHtmlAttributeDescriptorImpl.class);

    private final StencilDocComponent stencilDocComponent;

    public ExtendedHtmlAttributeDescriptorImpl(XmlAttributeDescriptor _delegate, boolean caseSensitive, XmlTag context, StencilMergedDoc mergedDoc) {
        super(_delegate, caseSensitive);
        this.stencilDocComponent = CompletionTypeUtil.traverseParentsToStencilElement(context, mergedDoc);
    }

    @Override
    public boolean isEnumerated() {
        if (hasStencilSlots()) {
            return true;
        }

        return super.isEnumerated();
    }

    @Override
    public String[] getEnumeratedValues() {
        return super.getEnumeratedValues();
    }

    @Override
    public String validateValue(XmlElement context, String value) {
        XmlElement parent = (XmlElement) context.getParent();
        if (parent.getText().contains("slot") && hasStencilSlots()) {
            List<String> slotNames = this.stencilDocComponent.getSlots().stream().map(s -> s.name).toList();
            boolean result = slotNames.stream().anyMatch(name -> name.equalsIgnoreCase(value));
            return result ? null : "Slot value should be one of " + String.join(", ", slotNames);
        }
        return super.validateValue(context, value);
    }

    private boolean hasStencilSlots() {
        return this.stencilDocComponent != null &&
                this.stencilDocComponent.getSlots() != null &&
                !this.stencilDocComponent.getSlots().isEmpty();
    }
}
