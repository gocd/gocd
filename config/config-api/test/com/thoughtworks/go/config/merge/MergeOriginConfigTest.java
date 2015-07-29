package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MergeOriginConfigTest {

    @Test
    public void shouldShowDisplayName()
    {
        FileConfigOrigin fileConfigOrigin = new FileConfigOrigin();
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(new ConfigRepoConfig(new SvnMaterialConfig("http://mysvn", false), "myplugin"), "123");
        MergeConfigOrigin mergeOrigin = new MergeConfigOrigin(fileConfigOrigin, repoConfigOrigin);
        assertThat(mergeOrigin.displayName(),is("Merged: [ cruise-config.xml; http://mysvn at 123; ]"));
    }


}
