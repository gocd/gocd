/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.commons.GoFileSystem;
import com.thoughtworks.go.plugin.infra.commons.PluginUploadResponse;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginWriterTest {

    @Mock
    private SystemEnvironment systemEnvironment;

    @Mock
    private GoFileSystem goFileSystem;

    @InjectMocks
    private PluginWriter pluginWriter = new PluginWriter(systemEnvironment, goFileSystem);

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String EXTERNAL_DIRECTORY_PATH = "external_path";
    private File srcFile;

    @Before
    public void init() throws IOException {
        initMocks(this);
        srcFile = temporaryFolder.newFile("a-plugin.jar");
    }

    @Test
    public void shouldConstructCorrectDestinationFilePath() throws Exception {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(EXTERNAL_DIRECTORY_PATH);

        pluginWriter.addPlugin(srcFile, srcFile.getName());

        ArgumentCaptor<File> srcfileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<File> destfileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(goFileSystem).copyFile(srcfileArgumentCaptor.capture(), destfileArgumentCaptor.capture());

        assertThat(srcfileArgumentCaptor.getValue(), is(srcFile));
        assertThat(destfileArgumentCaptor.getValue().getName(), is(new File(EXTERNAL_DIRECTORY_PATH + "/" + srcFile.getName()).getName()));
    }

    @Test
    public void shouldReturnSuccessResponseWhenSuccessfullyUploadedFile() throws Exception {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(EXTERNAL_DIRECTORY_PATH);

        PluginUploadResponse response = pluginWriter.addPlugin(srcFile, srcFile.getName());

        assertTrue(response.isSuccess());
        assertThat(response.success(), is("Your file is saved!"));
    }

    @Test
    public void shouldReturnErrorResponseWhenFailedToUploadFile() throws Exception {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(EXTERNAL_DIRECTORY_PATH);
        doThrow(new IOException()).when(goFileSystem).copyFile(any(File.class), any(File.class));

        PluginUploadResponse response = pluginWriter.addPlugin(srcFile, srcFile.getName());

        assertFalse(response.isSuccess());
        assertTrue(response.errors().containsKey(HttpStatus.SC_INTERNAL_SERVER_ERROR));

    }

}
