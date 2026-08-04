package com.github.glennlefevere.stenciljswebcomponents.completationProvider;

import com.intellij.codeInsight.lookup.LookupElementBuilder;

import com.intellij.ui.JBColor;
import javax.swing.*;
import java.net.URL;

public class IconUtil {

    public static LookupElementBuilder addIcon(LookupElementBuilder lookupElement) {
        String dark = JBColor.isBright() ? "" : "_dark";
        URL url = IconUtil.class.getClassLoader().getResource("preview_icon" + dark + ".png");
        if(url != null) {
            ImageIcon icon = new ImageIcon(url);
            return lookupElement.withIcon(icon);
        }
        return lookupElement;
    }
}
