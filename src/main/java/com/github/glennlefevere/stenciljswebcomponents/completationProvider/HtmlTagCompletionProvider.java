package com.github.glennlefevere.stenciljswebcomponents.completationProvider;

import com.github.glennlefevere.stenciljswebcomponents.listeners.StencilProjectListener;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilDocComponent;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.github.glennlefevere.stenciljswebcomponents.util.CompletionTypeUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class HtmlTagCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final Logger log = Logger.getInstance(HtmlTagCompletionProvider.class);

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext processingContext,
                                  @NotNull CompletionResultSet completionResultSet) {


        StencilMergedDoc mergedDoc = StencilProjectListener.INSTANCE.getStencilMergedDocForProject(parameters.getOriginalFile().getProject());
        if (CompletionTypeUtil.isTag(parameters) && mergedDoc != null && mergedDoc.getComponents() != null) {
            completionResultSet.addAllElements(
                    mergedDoc.getComponents().stream()
                            .map(component -> IconUtil.addIcon(LookupElementBuilder.create(component.tag)))
                            .collect(Collectors.toList()));
        } else if (mergedDoc != null && mergedDoc.getComponents() != null) {
            StencilDocComponent result = CompletionTypeUtil.isAttributeValueTokenOfSlotAndChildOfStencilElement(parameters, mergedDoc);
            if (result != null) {
                completionResultSet.addAllElements(result.getSlots()
                        .stream()
                        .map((slot) -> IconUtil.addIcon(LookupElementBuilder.create(slot.name)))
                        .toList());
            }
        }

    }
}
