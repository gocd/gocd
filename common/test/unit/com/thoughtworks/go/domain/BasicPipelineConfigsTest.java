package com.thoughtworks.go.domain;

import java.util.List;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.PipelineConfigsBaseTest;
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by tomzo on 6/23/15.
 */
public class BasicPipelineConfigsTest extends PipelineConfigsBaseTest {

    @Override
    protected PipelineConfigs createWithPipeline(PipelineConfig pipelineConfig) {
        return new BasicPipelineConfigs(pipelineConfig);
    }

    @Override
    protected PipelineConfigs createEmpty() {
        return new BasicPipelineConfigs();
    }

    @Override
    protected PipelineConfigs createWithPipelines(PipelineConfig first, PipelineConfig second) {
        return new BasicPipelineConfigs(first, second);
    }
    @Test
    public void shouldSetOriginInPipelines() {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs group = new BasicPipelineConfigs(pipe);
        group.setOrigins(new FileConfigOrigin());
        assertThat(pipe.getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }
    @Test
    public void shouldSetOriginInAuthorization() {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs group = new BasicPipelineConfigs(pipe);
        group.setOrigins(new FileConfigOrigin());
        assertThat(group.getAuthorization().getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }


    @Test
    public void shouldUpdateName() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(m(BasicPipelineConfigs.GROUP, "my-new-group"));
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(m());
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(null);
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(m(BasicPipelineConfigs.GROUP, null));
        assertThat(group.getGroup(), is(nullValue()));
    }


}
