package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.materials.MaterialPoller;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Date;

import static com.thoughtworks.go.domain.materials.Modification.modifications;
import static org.mockito.Mockito.*;

/**
 * Created by tomzo on 6/27/15.
 */
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
        MaterialPoller poller = mock(MaterialPoller.class);
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
