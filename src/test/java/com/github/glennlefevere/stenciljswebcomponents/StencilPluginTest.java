package com.github.glennlefevere.stenciljswebcomponents;

import com.github.glennlefevere.stenciljswebcomponents.descriptors.ExtendedHtmlAttributeDescriptorImpl;
import com.github.glennlefevere.stenciljswebcomponents.descriptors.ExtendedHtmlElementDescriptorImpl;
import com.github.glennlefevere.stenciljswebcomponents.model.StencilMergedDoc;
import com.github.glennlefevere.stenciljswebcomponents.services.StencilDocService;
import com.github.glennlefevere.stenciljswebcomponents.startup.StartupListener;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import com.intellij.xml.XmlAttributeDescriptor;
import org.junit.Test;

public class StencilPluginTest extends LightPlatformCodeInsightFixture4TestCase {

    private static final String FIXTURE_DOCS = """
            {
              "compiler": {
                "name": "@stencil/core"
              },
              "components": [
                {
                  "tag": "fixture-card",
                  "props": [
                    {
                      "name": "heading",
                      "required": false,
                      "default": null,
                      "values": [
                        {
                          "type": "string",
                          "value": null
                        }
                      ]
                    }
                  ],
                  "events": [
                    {
                      "event": "selected",
                      "detail": "string"
                    }
                  ],
                  "slots": [
                    {
                      "name": "content",
                      "docs": "Card content"
                    }
                  ]
                },
                {
                  "tag": "empty-card",
                  "props": [],
                  "events": [],
                  "slots": []
                },
                {
                  "tag": "absent-card",
                  "props": [],
                  "events": []
                }
              ]
            }
            """;

    private StencilDocService stencilDocService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject(
                "node_modules/fixture-components/package.json",
                """
                        {
                          "name": "fixture-components",
                          "dependencies": {
                            "@stencil/core": "4.0.0"
                          }
                        }
                        """);
        myFixture.addFileToProject(
                "node_modules/fixture-components/dist/docs/custom-elements-docs.json",
                FIXTURE_DOCS);

        stencilDocService = StencilDocService.getInstance(getProject());
        stencilDocService.refresh();
        PlatformTestUtil.waitWithEventsDispatching(
                "Stencil documentation was not loaded",
                this::hasFixtureComponent,
                10);
    }

    @Test
    public void testLoadsMergedDocumentation() {
        assertTrue(hasFixtureComponent());
    }

    @Test
    public void testRegistersBackgroundStartupActivity() {
        ExtensionPointName<ProjectActivity> extensionPoint =
                ExtensionPointName.create("com.intellij.backgroundPostStartupActivity");

        assertTrue(extensionPoint.getExtensionList().stream().anyMatch(StartupListener.class::isInstance));
    }

    @Test
    public void testProvidesDescriptorsForStencilAndHtmlElements() {
        WebComponentDescriptorsProvider provider = new WebComponentDescriptorsProvider();

        assertInstanceOf(
                provider.getDescriptor(configureRootTag("<fixture-card></fixture-card>")),
                StencilTagDescriptor.class);
        assertInstanceOf(
                provider.getDescriptor(configureRootTag("<div></div>")),
                ExtendedHtmlElementDescriptorImpl.class);
        assertNull(provider.getDescriptor(configureRootTag("<unknown-element></unknown-element>")));
    }

    @Test
    public void testProvidesPropertyAndEventDescriptors() {
        XmlTag tag = configureRootTag("<fixture-card></fixture-card>");
        WebComponentAttributeDescriptorsProvider provider = new WebComponentAttributeDescriptorsProvider();

        assertNotNull(provider.getAttributeDescriptor("heading", tag));
        assertNotNull(provider.getAttributeDescriptor("selected", tag));
    }

    @Test
    public void testCompletesStencilTagsAndSlots() {
        myFixture.configureByText("tag-completion.html", "<<caret>");
        myFixture.completeBasic();
        assertContainsElements(myFixture.getLookupElementStrings(), "fixture-card");

        myFixture.configureByText(
                "slot-completion.html",
                "<fixture-card><div slot=\"<caret>\"></div></fixture-card>");
        myFixture.completeBasic();
        assertContainsElements(myFixture.getLookupElementStrings(), "content");
    }

    @Test
    public void testValidatesKnownSlots() {
        SlotDescriptorFixture fixture = createSlotDescriptorFixture("fixture-card");

        assertTrue(fixture.descriptor().isEnumerated());
        assertNull(fixture.descriptor().validateValue(fixture.valueElement(), "content"));
        assertEquals(
                "Slot value should be one of content",
                fixture.descriptor().validateValue(fixture.valueElement(), "unknown"));
    }

    @Test
    public void testFallsBackForEmptyAndAbsentSlots() {
        assertUsesDefaultSlotBehavior("empty-card");
        assertUsesDefaultSlotBehavior("absent-card");
    }

    private boolean hasFixtureComponent() {
        StencilMergedDoc mergedDoc = stencilDocService.getMergedDoc();
        return mergedDoc != null &&
                mergedDoc.getComponents() != null &&
                mergedDoc.getComponents().stream()
                        .anyMatch(component -> "fixture-card".equals(component.getTag()));
    }

    private XmlTag configureRootTag(String contents) {
        PsiFile file = myFixture.configureByText("descriptor.html", contents);
        return ((XmlFile) file).getRootTag();
    }

    private SlotDescriptorFixture createSlotDescriptorFixture(String componentTag) {
        XmlTag component = configureRootTag(
                "<" + componentTag + "><div slot=\"unknown\"></div></" + componentTag + ">");
        XmlTag child = component.getSubTags()[0];
        XmlAttribute slot = child.getAttribute("slot");
        assertNotNull(slot);
        XmlElement valueElement = slot.getValueElement();
        assertNotNull(valueElement);

        XmlAttributeDescriptor defaultDescriptor =
                HtmlNSDescriptorImpl.getCommonAttributeDescriptor("slot", child);
        assertNotNull(defaultDescriptor);
        ExtendedHtmlAttributeDescriptorImpl descriptor = new ExtendedHtmlAttributeDescriptorImpl(
                defaultDescriptor,
                false,
                child,
                stencilDocService.getMergedDoc());
        return new SlotDescriptorFixture(descriptor, defaultDescriptor, valueElement);
    }

    private void assertUsesDefaultSlotBehavior(String componentTag) {
        SlotDescriptorFixture fixture = createSlotDescriptorFixture(componentTag);
        assertEquals(fixture.defaultDescriptor().isEnumerated(), fixture.descriptor().isEnumerated());
        assertEquals(
                fixture.defaultDescriptor().validateValue(fixture.valueElement(), "unknown"),
                fixture.descriptor().validateValue(fixture.valueElement(), "unknown"));
    }

    private record SlotDescriptorFixture(
            ExtendedHtmlAttributeDescriptorImpl descriptor,
            XmlAttributeDescriptor defaultDescriptor,
            XmlElement valueElement) {
    }
}
