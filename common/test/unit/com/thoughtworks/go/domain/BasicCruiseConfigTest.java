package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BasicCruiseConfigTest extends CruiseConfigTestBase {

    @Before
    public void setup() throws Exception {
        pipelines = new BasicPipelineConfigs("existing_group", new Authorization());
        cruiseConfig = new BasicCruiseConfig(pipelines);
        goConfigMother = new GoConfigMother();
    }

    @Override
    protected BasicCruiseConfig createCruiseConfig(BasicPipelineConfigs pipelineConfigs) {
        return new BasicCruiseConfig(pipelineConfigs);
    }

    @Override
    protected BasicCruiseConfig createCruiseConfig() {
        return new BasicCruiseConfig();
    }

    @Test
    public void shouldGenerateAMapOfAllPipelinesAndTheirParentDependencies() {
        /*
        *    -----+ p2 --> p4
        *  p1
        *    -----+ p3
        *
        * */
        PipelineConfig p1 = createPipelineConfig("p1", "s1", "j1");
        PipelineConfig p2 = createPipelineConfig("p2", "s2", "j1");
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p3 = createPipelineConfig("p3", "s3", "j1");
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p4 = createPipelineConfig("p4", "s4", "j1");
        p4.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"), new CaseInsensitiveString("s2")));
        pipelines.addAll(Arrays.asList(p4, p2, p1, p3));
        Map<String, List<PipelineConfig>> expectedPipelines = cruiseConfig.generatePipelineVsDownstreamMap();
        assertThat(expectedPipelines.size(), is(4));
        assertThat(expectedPipelines.get("p1"), hasItems(p2, p3));
        assertThat(expectedPipelines.get("p2"), hasItems(p4));
        assertThat(expectedPipelines.get("p3").isEmpty(), is(true));
        assertThat(expectedPipelines.get("p4").isEmpty(), is(true));
    }


    @Test
    public void shouldSetOriginInPipelines() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), PipelineConfigMother.pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        PipelineConfig pipe = pipelines.get(0);
        mainCruiseConfig.setOrigins(new FileConfigOrigin());
        assertThat(pipe.getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }
    @Test
    public void shouldSetOriginInEnvironments() {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        BasicEnvironmentConfig env = new BasicEnvironmentConfig(new CaseInsensitiveString("e"));
        mainCruiseConfig.addEnvironment(env);
        mainCruiseConfig.setOrigins(new FileConfigOrigin());
        assertThat(env.getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }


    @Test
    public void shouldGetPipelinesWithGroupName() throws Exception {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        PipelineConfigs group2 = createGroup("group2", createPipelineConfig("pipeline2", "stage2"));
        CruiseConfig config = createCruiseConfig();
        config.setGroup(new PipelineGroups(group1, group2));


        assertThat(config.pipelines("group1"), is(group1));
        assertThat(config.pipelines("group2"), is(group2));
    }

    @Test
    public void shouldReturnTrueForPipelineThatInFirstGroup() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        CruiseConfig config = new BasicCruiseConfig(group1);
        assertThat("shouldReturnTrueForPipelineThatInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline1")), is(true));
    }

    @Test
    public void shouldReturnFalseForPipelineThatNotInFirstGroup() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        PipelineConfigs group2 = createGroup("group2", createPipelineConfig("pipeline2", "stage2"));
        CruiseConfig config = new BasicCruiseConfig(group1, group2);
        assertThat("shouldReturnFalseForPipelineThatNotInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline2")), is(false));
    }


}
