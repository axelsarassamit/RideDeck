package com.axelsarassamit.gx12;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.*;
import static org.junit.Assert.*;

/** Native dialogs and fields inherit XML themes, while pages use the runtime palette. */
public final class NativeAppearanceContractTest {
    private File resources() {
        File local = new File("src/main/res");
        return local.isDirectory() ? local : new File("app/src/main/res");
    }
    private Document xml(String name) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new File(resources(), name));
    }
    private int colour(String value) {
        assertTrue("Expected a literal family colour, got " + value, value.matches("#[0-9a-fA-F]{6}"));
        return (int)(Long.parseLong(value.substring(1),16) | 0xff000000L);
    }
    private int item(String style, String key) throws Exception {
        NodeList styles=xml("values/styles.xml").getElementsByTagName("style");
        for(int i=0;i<styles.getLength();i++) {
            Element element=(Element)styles.item(i);
            if(!style.equals(element.getAttribute("name")))continue;
            NodeList items=element.getElementsByTagName("item");
            for(int j=0;j<items.getLength();j++) {
                Element value=(Element)items.item(j);
                if(key.equals(value.getAttribute("name")))return colour(value.getTextContent());
            }
        }
        throw new AssertionError("Missing theme item: "+style+"/"+key);
    }
    @Test public void nativeThemesMatchRuntimeInLightAndDark() throws Exception {
        for(boolean dark:new boolean[]{false,true}) {
            String page=dark?"RideDeckTheme":"RideDeckLightTheme";
            String dialog=dark?"RideDeckDialog":"RideDeckLightDialog";
            assertEquals(AppearancePalette.background(dark),item(page,"android:windowBackground"));
            assertEquals(AppearancePalette.background(dark),item(page,"android:colorBackground"));
            assertEquals(AppearancePalette.panel(dark),item(dialog,"android:colorBackground"));
            for(String style:new String[]{page,dialog}) {
                assertEquals(AppearancePalette.text(dark),item(style,"android:textColorPrimary"));
                assertEquals(AppearancePalette.muted(dark),item(style,"android:textColorSecondary"));
            }
            String surface=dark?"drawable/dialog_surface.xml":"drawable/dialog_surface_light.xml";
            Document shape=xml(surface);
            String namespace="http://schemas.android.com/apk/res/android";
            assertEquals(AppearancePalette.panel(dark),colour(((Element)shape.getElementsByTagName("solid").item(0)).getAttributeNS(namespace,"color")));
            assertEquals(AppearancePalette.line(dark),colour(((Element)shape.getElementsByTagName("stroke").item(0)).getAttributeNS(namespace,"color")));
        }
    }
}
