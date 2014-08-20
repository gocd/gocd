package com.thoughtworks.go.server.util;

import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mortbay.jetty.handler.ContextHandler;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RailsAssetsHelperTest {

    private File assetsDir;

    @Before
    public void setup() {
        assetsDir = FileUtil.createTempFolder("assets-" + UUID.randomUUID().toString());
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(assetsDir);
    }

    @Test
    public void shouldGetAssetPathFromManifestJson() throws IOException {
        ContextHandler.SContext context = mock(ContextHandler.SContext.class);
        FileUtil.writeContentToFile(json, new File(assetsDir, "manifest-digest.json"));
        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath(Matchers.<String>any())).thenReturn(assetsDir.getAbsolutePath());
        RailsAssetsHelper helper = new RailsAssetsHelper(context);
        assertThat(helper.getAssetPath("application.js"), is("assets/application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js"));
        assertThat(helper.getAssetPath("junk.js"), is(nullValue()));
    }

    @Test
    public void shouldThrowExceptionIfManifestFileIsNotFoundInAssetsDir() throws IOException {
        ContextHandler.SContext context = mock(ContextHandler.SContext.class);
        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath(Matchers.<String>any())).thenReturn(assetsDir.getAbsolutePath());
        try{
            new RailsAssetsHelper(context);
            fail("Expected exception to be thrown");
        }
        catch (Exception e){
            assertThat(e.getMessage(), is("Manifest json file was not found at " + assetsDir.getAbsolutePath()));
        }
    }

    @Test
    public void shouldThrowExceptionIfAssetsDirDoesNotExist() throws IOException {
        ContextHandler.SContext context = mock(ContextHandler.SContext.class);
        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath(Matchers.<String>any())).thenReturn("DoesNotExist");
        try{
            new RailsAssetsHelper(context);
            fail("Expected exception to be thrown");
        }
        catch (Exception e){
            assertThat(e.getMessage(), is("Assets directory does not exist DoesNotExist"));
        }
    }

    private String json = "{\n" +
            "    \"files\": {\n" +
            "        \"application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js\": {\n" +
            "            \"logical_path\": \"application.js\",\n" +
            "            \"mtime\": \"2014-08-26T12:39:43+05:30\",\n" +
            "            \"size\": 1091366,\n" +
            "            \"digest\": \"bfdbd4fff63b0cd45c50ce7a79fe0f53\"\n" +
            "        },\n" +
            "        \"application-4b25c82f986c0bef78151a4ab277c3e4.css\": {\n" +
            "            \"logical_path\": \"application.css\",\n" +
            "            \"mtime\": \"2014-08-26T13:45:30+05:30\",\n" +
            "            \"size\": 513,\n" +
            "            \"digest\": \"4b25c82f986c0bef78151a4ab277c3e4\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"assets\": {\n" +
            "        \"application.js\": \"application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js\",\n" +
            "        \"application.css\": \"application-4b25c82f986c0bef78151a4ab277c3e4.css\"\n" +
            "    }\n" +
            "}";


}