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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.materials.MaterialPoller;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Date;

import static com.thoughtworks.go.domain.materials.Modification.modifications;
import static org.mockito.Mockito.*;

public class ConfigMaterialUpdaterTest {
    private GoRepoConfigDataSource repoConfigDataSource;
    private MaterialRepository materialRepository;
    private MaterialChecker materialChecker;
    private ConfigMaterialUpdateCompletedTopic configCompleted;
    private MaterialUpdateCompletedTopic topic;
    private ConfigMaterialUpdater configUpdater;
    private   MaterialService materialService;

    private Material material;
    private File folder = new File("checkoutDir");
    private  MaterialRevisions mods;
    private MaterialPoller poller;

    @Before
    public void SetUp()
    {
        repoConfigDataSource = mock(GoRepoConfigDataSource.class);
        materialChecker = mock(MaterialChecker.class);
        materialRepository = mock(MaterialRepository.class);
        configCompleted = mock(ConfigMaterialUpdateCompletedTopic.class);
        topic = mock(MaterialUpdateCompletedTopic.class);
        materialService = mock(MaterialService.class);

        material = new SvnMaterial("url","tom","pass",false);

        when(materialRepository.folderFor(material)).thenReturn(folder);
        poller = mock(MaterialPoller.class);
        when(materialService.getPollerImplementation(any(Material.class))).thenReturn(poller);

        Modification svnModification = new Modification("user", "commend", "em@il", new Date(), "1");
        mods = revisions(material,svnModification);

        when(materialRepository.findLatestModification(material)).thenReturn(mods);

        configUpdater = new ConfigMaterialUpdater(
                repoConfigDataSource,materialRepository,materialChecker,
                configCompleted,topic,materialService,new TestSubprocessExecutionContext());
    }
    private MaterialRevisions revisions(Material material, Modification modification) {
        return new MaterialRevisions(new MaterialRevision(material, modifications(modification)));
    }

    @Test
    public void shouldSubscribeToMaterialUpdateCompletedMessages()
    {
        verify(configCompleted,times(1)).addListener(configUpdater);
    }

    @Test
    public void shouldPostMaterialUpdateCompletedMessagesFurther()
    {
        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(topic,times(1)).post(message);
    }

    @Test
    public void shouldPerformCheckoutUsingMaterialPoller()
    {
        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(poller,times(1)).checkout(any(Material.class),any(File.class), any(Revision.class),any(SubprocessExecutionContext.class));
    }

    @Test
    public void shouldCallGoRepoConfigDataSourceWhenMaterialUpdateSuccessfulMessage()
    {
        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(repoConfigDataSource,times(1)).onCheckoutComplete(material.config(),folder,"1");
        verify(topic,times(1)).post(message);
    }
    @Test
    public void shouldNotCallGoRepoConfigDataSourceWhenMaterialUpdateFailedMessage()
    {
        MaterialUpdateFailedMessage message = new MaterialUpdateFailedMessage(material, 123, new RuntimeException("bla"));
        this.configUpdater.onMessage(message);

        verify(repoConfigDataSource,times(0)).onCheckoutComplete(material.config(),folder,"1");
        verify(topic,times(1)).post(message);
    }

    @Test
    public void shouldNotCallGoRepoConfigDataSourceWhenNoChanges()
    {
        when(repoConfigDataSource.getRevisionAtLastAttempt(material.config())).thenReturn("1");
        when(materialChecker.findSpecificRevision(material,"1")).thenReturn(mods.getMaterialRevision(0));

        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(repoConfigDataSource,times(0)).onCheckoutComplete(material.config(),folder,"1");
        // but pass message further anyway
        verify(topic,times(1)).post(message);
    }
    @Test
    public void shouldCallGoRepoConfigDataSourceWhenNewRevision()
    {
        when(repoConfigDataSource.getRevisionAtLastAttempt(material.config())).thenReturn("1");
        when(materialChecker.findSpecificRevision(material,"1")).thenReturn(mods.getMaterialRevision(0));

        Modification svnModification = new Modification("user", "commend", "em@il", new Date(), "2");
        MaterialRevisions mods2 = revisions(material, svnModification);
        when(materialRepository.findLatestModification(material)).thenReturn(mods2);

        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(repoConfigDataSource,times(1)).onCheckoutComplete(material.config(),folder,"2");
        verify(topic,times(1)).post(message);
    }
}
