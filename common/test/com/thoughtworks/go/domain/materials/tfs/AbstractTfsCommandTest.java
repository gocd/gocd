/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.materials.tfs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractTfsCommandTest {

    private UrlArgument url = new UrlArgument("url");
    private String domain = "domain";
    private String user = "user";
    private String password = "password";
    private String workspace = "workspace";
    private String projectPath = "projectPath";
    private File workDir = FileUtil.createTempFolder();
    private StringRevision revision = new StringRevision("1");
    AbstractTfsCommand tfsCommand;

    @Before
    public void setUp() {
        tfsCommand = spy(new AbstractTfsCommand(null, url, domain, user, password, workspace, projectPath) {
            @Override protected void unMap() throws IOException {
            }

            @Override protected List<Modification> history(String beforeRevision, long revsToLoad) {
                return Arrays.asList(new Modification());
            }

            @Override protected void retrieveFiles(File workDir, Revision revision) {
            }

            @Override protected void initializeWorkspace(File workDir) {
            }
        });
        assertThat(workDir.exists(), is(true));
    }

    public void verifyMocks() throws IOException {
        verify(tfsCommand, times(1)).initializeWorkspace(workDir);
        verify(tfsCommand, times(1)).unMap();
    }

    @Test
    public void testCheckout() throws Exception {
        tfsCommand.checkout(workDir, revision);
        verify(tfsCommand, times(1)).retrieveFiles(workDir, revision);
        verifyMocks();
    }

    @Test
    public void testLatestModification() throws Exception {
        tfsCommand.latestModification(workDir);
        verify(tfsCommand, times(1)).history(null, 1);
    }

    @Test
    public void testModificationsSince() throws Exception {
        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add(new Modification(user, "comment latest", "email", new Date(), "10"));
        modifications.add(new Modification(user, "comment latest", "email", new Date(), "9"));
        modifications.add(new Modification(user, "comment latest", "email", new Date(), "8"));

        when(tfsCommand.history(null, 1)).thenReturn(Arrays.asList(modifications.get(0)));
        when(tfsCommand.history("10", 3)).thenReturn(modifications);
        List<Modification> actual = tfsCommand.modificationsSince(workDir, new StringRevision("7"));

        assertThat(actual.containsAll(modifications), is(true));
    }

    @Test
    public void testCheckConnection() throws Exception {
        tfsCommand.checkConnection();
    }
}
