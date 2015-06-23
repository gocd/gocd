package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.PipelineConfigsBaseTest;
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.helper.PipelineConfigMother;
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
