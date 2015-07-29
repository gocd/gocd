package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RepoConfigOriginTest {

    @Test
    public void shouldShowDisplayName()
    {
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(new ConfigRepoConfig(new SvnMaterialConfig("http://mysvn", false), "myplugin"), "123");
        assertThat(repoConfigOrigin.displayName(),is("http://mysvn at 123"));
    }

    // because we don't like null pointer exceptions
    // and empty config like this can happen in tests
    @Test
    public void shouldShowDisplayNameWhenEmptyConfig()
    {
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin();
        assertThat(repoConfigOrigin.displayName(),is("NULL material at null"));
    }


}
