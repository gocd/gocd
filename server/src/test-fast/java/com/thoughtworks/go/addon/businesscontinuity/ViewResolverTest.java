package com.thoughtworks.go.addon.businesscontinuity;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class ViewResolverTest {

    @Test
    void shouldResolveView() {
        String template = "<html><head><link href=\"<<<key1>>>\\<<<key2>>>\"/></head></html>";
        HashMap<String, String> modelMap = new HashMap<String, String>();
        modelMap.put("key1", "value1");
        modelMap.put("key2", "value2");

        ViewResolver viewResolverSpy = spy(new ViewResolver());
        doReturn(IOUtils.toInputStream(template)).when(viewResolverSpy).getResourceAsStream("sample");
        String resolvedView = viewResolverSpy.resolveView("sample", modelMap);

        assertThat(resolvedView, is("<html><head><link href=\"value1\\value2\"/></head></html>"));
    }
}
