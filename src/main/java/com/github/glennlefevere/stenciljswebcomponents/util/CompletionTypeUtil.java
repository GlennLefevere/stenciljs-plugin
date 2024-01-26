package com.github.glennlefevere.stenciljswebcomponents.util;

import com.github.glennlefevere.stenciljswebcomponents.model.StencilDocComponent;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.xml.XmlToken;

import java.util.Optional;

public class CompletionTypeUtil {
    private static final Logger log = Logger.getInstance(CompletionTypeUtil.class);

    public static boolean isTag(CompletionParameters parameters) {
        PsiElement position = parameters.getPosition().getPrevSibling();
        if (position != null) {
            return position.getText().equals("<");
        }
        return false;
    }

    public static StencilDocComponent isAttributeValueTokenOfSlotAndChildOfStencilElement(CompletionParameters parameters, StencilMergedDoc mergedDoc) {
        if(parameters.getPosition() instanceof XmlToken token) {
            if(token.getTokenType().toString().equalsIgnoreCase("XML_ATTRIBUTE_VALUE_TOKEN") && token.getParent().getParent().getFirstChild().getText().equalsIgnoreCase("slot")) {
                return traverseParentsToStencilElement(token.getParent(), mergedDoc);
            }
        }

        return null;
    }

    private static StencilDocComponent traverseParentsToStencilElement(PsiElement token, StencilMergedDoc mergedDoc) {
        if(token != null && !(token instanceof HtmlTag)) {
            return traverseParentsToStencilElement(token.getParent(), mergedDoc);
        } else if(token != null) {
            HtmlTag tag = (HtmlTag) token;
            Optional<StencilDocComponent> optionalStencilDocComponent = mergedDoc.getComponents().stream()
                    .filter((c) -> c.tag.equalsIgnoreCase(tag.getName()))
                    .findAny();

            if (optionalStencilDocComponent.isPresent()) {
                StencilDocComponent stencilDocComponent = optionalStencilDocComponent.get();
                if (!stencilDocComponent.slots.isEmpty()) {
                    return stencilDocComponent;
                }
            } else {
                return traverseParentsToStencilElement(token.getParent(), mergedDoc);
            }
        }

        return null;
    }

}
