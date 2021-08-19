package com.thoughtworks.go.addon.businesscontinuity;

import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PluginsListTest {

    @Mock
    private SystemEnvironment systemEnvironment;
    private PluginsList pluginsList;
    private File bundledPluginsDir;
    private File externalPluginsDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        bundledPluginsDir = TempDirUtils.createTempDirectoryIn(tempDir, "bundled").toFile();
        externalPluginsDir = TempDirUtils.createTempDirectoryIn(tempDir, "external").toFile();

        File bundledYumPlugin = new File(bundledPluginsDir, "yum.jar");
        File externalPlugin1 = new File(externalPluginsDir, "external1.jar");
        File externalPlugin2 = new File(externalPluginsDir, "external2.jar");
        File externalPlugin3 = new File(externalPluginsDir, "external3.jar");
        FileUtils.writeStringToFile(bundledYumPlugin, bundledYumPlugin.getName(), UTF_8);
        FileUtils.writeStringToFile(externalPlugin1, externalPlugin1.getName(), UTF_8);
        FileUtils.writeStringToFile(externalPlugin2, externalPlugin2.getName(), UTF_8);
        FileUtils.writeStringToFile(externalPlugin3, externalPlugin3.getName(), UTF_8);

        when(systemEnvironment.getBundledPluginAbsolutePath()).thenReturn(bundledPluginsDir.getAbsolutePath());
        when(systemEnvironment.getExternalPluginAbsolutePath()).thenReturn(externalPluginsDir.getAbsolutePath());

        pluginsList = new PluginsList(systemEnvironment, mock(PluginManager.class));
    }

    @Test
    void shouldUpdatePluginsList() {
        pluginsList.update();
        PluginsList.PluginEntries bundled = (PluginsList.PluginEntries) ReflectionUtil.getField(pluginsList, "bundled");
        PluginsList.PluginEntries external = (PluginsList.PluginEntries) ReflectionUtil.getField(pluginsList, "external");

        assertThat(bundled.size(), is(1));
        assertThat(bundled.get(0).getName(), is("yum.jar"));
        assertThat(bundled.get(0).getMd5(), is("2c05416f0683ca4ae27039c03f9ee496"));

        assertThat(external.size(), is(3));
        assertThat(external.get(0).getName(), is("external1.jar"));
        assertThat(external.get(0).getMd5(), is("fb25832b8fad610b5fab2877b664fde4"));
        assertThat(external.get(1).getName(), is("external2.jar"));
        assertThat(external.get(1).getMd5(), is("0d2fce6b4befe5446d7977f3494ee6bd"));
        assertThat(external.get(2).getName(), is("external3.jar"));
        assertThat(external.get(2).getMd5(), is("39057ae38c2c04981bb50591f8bdd45c"));
    }

    @Test
    void shouldHaveThePluginsListedInSortedOrderBasedOnNameInPluginsJSON() throws Exception {
        FileUtils.cleanDirectory(externalPluginsDir);
        FileUtil.createFilesByPath(externalPluginsDir, "foo", "bar", "baz", "ano");
        pluginsList.update();

        String pluginsJSON = pluginsList.getPluginsJSON();

        PluginsList deserialized = JsonHelper.fromJson(pluginsJSON, PluginsList.class);
        PluginsList.PluginEntries externalPlugins = (PluginsList.PluginEntries) ReflectionUtil.getField(deserialized, "external");

        assertThat(externalPlugins.size(), is(4));
        assertThat(externalPlugins.get(0).getName(), is("ano"));
        assertThat(externalPlugins.get(1).getName(), is("bar"));
        assertThat(externalPlugins.get(2).getName(), is("baz"));
        assertThat(externalPlugins.get(3).getName(), is("foo"));
    }

    @Test
    void shouldRemoveOlderEntriesDuringUpdate() {
        pluginsList.update();
        PluginsList.PluginEntries bundled = (PluginsList.PluginEntries) ReflectionUtil.getField(pluginsList, "bundled");
        assertThat(bundled.size(), is(1));

        File bundledYumPlugin = new File(bundledPluginsDir, "yum.jar");
        FileUtils.deleteQuietly(bundledYumPlugin);

        pluginsList.update();
        assertThat(bundled.isEmpty(), is(true));
    }

    @Test
    void shouldGetPluginsJSON() {
        pluginsList.update();
        assertThat(ReflectionUtil.getField(pluginsList, "pluginsJSON"), is(nullValue()));

        String allPluginsJSON = pluginsList.getPluginsJSON();

        assertThat(ReflectionUtil.getField(pluginsList, "pluginsJSON"), is(notNullValue()));
        assertThat(allPluginsJSON, is("{\"bundled\":[{\"name\":\"yum.jar\",\"md5\":\"2c05416f0683ca4ae27039c03f9ee496\"}],\"external\":[{\"name\":\"external1.jar\",\"md5\":\"fb25832b8fad610b5fab2877b664fde4\"},{\"name\":\"external2.jar\",\"md5\":\"0d2fce6b4befe5446d7977f3494ee6bd\"},{\"name\":\"external3.jar\",\"md5\":\"39057ae38c2c04981bb50591f8bdd45c\"}]}"));
    }
}
