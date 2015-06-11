package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by tomzo on 6/11/15.
 */
public class MergeEnvironmentConfigTest {
    public MergeEnvironmentConfig singleEnvironmentConfig;
    private static final String AGENT_UUID = "uuid";

    @Before
    public void setUp() throws Exception {
        singleEnvironmentConfig = new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("One")));
    }

    @Test
    public void shouldCreateMatcherWhenNoPipelines() throws Exception {
        EnvironmentPipelineMatcher pipelineMatcher = singleEnvironmentConfig.createMatcher();
        assertThat(pipelineMatcher.match("pipeline", AGENT_UUID), is(false));
    }

    @Test
    public void shouldCreateMatcherWhenPipelinesGiven() throws Exception {
        singleEnvironmentConfig.first().addPipeline(new CaseInsensitiveString("pipeline"));
        singleEnvironmentConfig.first().addAgent(AGENT_UUID);
        EnvironmentPipelineMatcher pipelineMatcher = singleEnvironmentConfig.createMatcher();
        assertThat(pipelineMatcher.match("pipeline", AGENT_UUID), is(true));
    }
}
