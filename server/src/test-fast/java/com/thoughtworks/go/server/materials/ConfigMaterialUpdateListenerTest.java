/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.GoConfigRepoConfigDataSource;
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
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Date;

import static com.thoughtworks.go.domain.materials.Modification.modifications;
import static org.mockito.Mockito.*;

public class ConfigMaterialUpdateListenerTest {
    private GoConfigRepoConfigDataSource repoConfigDataSource;
    private MaterialRepository materialRepository;
    private MaterialUpdateCompletedTopic topic;
    private ConfigMaterialUpdateListener configUpdater;
    private MaterialService materialService;
    private Material material;
    private final File folder = new File("checkoutDir");
    private Modification svnModification;

    @Before
    public void SetUp() {
        repoConfigDataSource = mock(GoConfigRepoConfigDataSource.class);
        materialRepository = mock(MaterialRepository.class);
        topic = mock(MaterialUpdateCompletedTopic.class);
        materialService = mock(MaterialService.class);

        material = new SvnMaterial("url", "tom", "pass", false);

        when(materialRepository.folderFor(material)).thenReturn(folder);

        svnModification = new Modification("user", "commend", "em@il", new Date(), "1");
        MaterialRevisions mods = revisions(material, svnModification);

        when(materialRepository.findLatestModification(material)).thenReturn(mods);

        configUpdater = new ConfigMaterialUpdateListener(repoConfigDataSource, materialRepository,
                topic, materialService, new TestSubprocessExecutionContext());
    }

    private MaterialRevisions revisions(Material material, Modification modification) {
        return new MaterialRevisions(new MaterialRevision(material, modifications(modification)));
    }

    @Test
    public void shouldPostMaterialUpdateCompletedMessagesFurther() {
        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(topic, times(1)).post(message);
    }

    @Test
    public void shouldCheckoutMaterialToASpecificRevision() {
        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);

        this.configUpdater.onMessage(message);

        verify(materialService, times(1)).checkout(any(Material.class), any(File.class), any(Revision.class), any(SubprocessExecutionContext.class));
    }

    @Test
    public void shouldCallGoRepoConfigDataSourceWhenMaterialUpdateSuccessfulMessage() {
        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(repoConfigDataSource, times(1)).onCheckoutComplete(material.config(), folder, svnModification);
        verify(topic, times(1)).post(message);
    }

    @Test
    public void shouldNotCallGoRepoConfigDataSourceWhenMaterialUpdateFailedMessage() {
        MaterialUpdateFailedMessage message = new MaterialUpdateFailedMessage(material, 123, new RuntimeException("bla"));
        this.configUpdater.onMessage(message);

        verify(repoConfigDataSource, times(0)).onCheckoutComplete(material.config(), folder, getModificationFor("1"));
        verify(topic, times(1)).post(message);
    }

    @Test
    public void shouldCallGoRepoConfigDataSourceWhenNewRevision() {
        when(repoConfigDataSource.getRevisionAtLastAttempt(material.config())).thenReturn("1");

        Modification svnModification = new Modification("user", "commend", "em@il", new Date(), "2");
        MaterialRevisions mods2 = revisions(material, svnModification);
        when(materialRepository.findLatestModification(material)).thenReturn(mods2);

        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(repoConfigDataSource, times(1)).onCheckoutComplete(material.config(), folder, svnModification);
        verify(topic, times(1)).post(message);
    }

    @Test
    public void shouldForceUpdateConfigurationOnChangeOfConfigRepoConfig() {
        Modification modification = new Modification("user", "commend", "em@il", new Date(), "1");
        MaterialRevisions materialRevisions = revisions(material, modification);

        when(repoConfigDataSource.getRevisionAtLastAttempt(material.config())).thenReturn("1");
        when(repoConfigDataSource.hasConfigRepoConfigChangedSinceLastUpdate(material.config())).thenReturn(true);
        when(materialRepository.findLatestModification(material)).thenReturn(materialRevisions);

        MaterialUpdateSuccessfulMessage message = new MaterialUpdateSuccessfulMessage(material, 123);
        this.configUpdater.onMessage(message);

        verify(repoConfigDataSource).onCheckoutComplete(material.config(), folder, modification);
        verify(topic, times(1)).post(message);
    }

    private Modification getModificationFor(String revision) {
        Modification modification = new Modification();
        modification.setRevision(revision);
        return modification;
    }
}
