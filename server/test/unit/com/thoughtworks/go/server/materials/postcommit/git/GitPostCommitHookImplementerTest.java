package com.thoughtworks.go.server.materials.postcommit.git;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class GitPostCommitHookImplementerTest {

    private GitPostCommitHookImplementer implementer;

    @Before
    public void setUp() throws Exception {
        implementer = new GitPostCommitHookImplementer();
    }

    @Test
    public void shouldReturnListOfMaterialMatchingThePayloadURL() throws Exception {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://other_repo.local.git"));
        GitMaterial material2 = mock(GitMaterial.class);
        when(material2.getUrlArgument()).thenReturn(new UrlArgument("https://other_repo.local.git"));
        GitMaterial material3 = mock(GitMaterial.class);
        when(material3.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        GitMaterial material4 = mock(GitMaterial.class);
        when(material4.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = new HashSet<Material>(Arrays.asList(material1, material2, material3, material4));
        HashMap params = new HashMap();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "https://machine.local.git");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(2));
        assertThat(actual, hasItem(material3));
        assertThat(actual, hasItem(material4));

        verify(material1).getUrlArgument();
        verify(material2).getUrlArgument();
        verify(material3).getUrlArgument();
        verify(material4).getUrlArgument();
    }

    @Test
    public void shouldQueryOnlyGitMaterialsWhilePruning() throws Exception {
        SvnMaterial material1 = mock(SvnMaterial.class);
        Set<Material> materials = new HashSet<Material>(Arrays.asList(material1));
        HashMap params = new HashMap();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "https://machine.local.git");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(0));

        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnEmptyListIfParamHasNoValueForRepoURL() throws Exception {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = new HashSet<Material>(Arrays.asList(material1));
        HashMap params = new HashMap();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(0));

        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnEmptyListIfParamIsMissingForRepoURL() throws Exception {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = new HashSet<Material>(Arrays.asList(material1));

        Set<Material> actual = implementer.prune(materials, new HashMap());

        assertThat(actual.size(), is(0));

        verifyNoMoreInteractions(material1);
    }
}