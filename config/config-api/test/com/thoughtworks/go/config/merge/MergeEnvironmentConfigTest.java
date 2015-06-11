package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MergeEnvironmentConfigTest {
    public MergeEnvironmentConfig singleEnvironmentConfig;
    public MergeEnvironmentConfig pairEnvironmentConfig;
    private static final String AGENT_UUID = "uuid";

    @Before
    public void setUp() throws Exception {
        singleEnvironmentConfig = new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("One")));
        pairEnvironmentConfig = new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("One")),new BasicEnvironmentConfig(new CaseInsensitiveString("One")));
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

    @Test
    public void twoEnvironmentConfigsShouldBeEqualIfNameIsEqual() throws Exception {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("One"));
        assertThat(another, Matchers.<EnvironmentConfig>is(singleEnvironmentConfig));
    }

    @Test
    public void twoEnvironmentConfigsShouldNotBeEqualIfnameNotEqual() throws Exception {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("other"));
        assertThat(another, Matchers.<EnvironmentConfig>is(Matchers.<EnvironmentConfig>not(singleEnvironmentConfig)));
    }
    @Test
    public void shouldAddEnvironmentVariablesToEnvironmentVariableContext() throws Exception {
        singleEnvironmentConfig.addEnvironmentVariable("variable-name", "variable-value");
        EnvironmentVariableContext context = singleEnvironmentConfig.createEnvironmentContext();
        assertThat(context.getProperty("variable-name"), is("variable-value"));
    }

    @Test
    public void shouldAddEnvironmentNameToEnvironmentVariableContext() throws Exception {
        EnvironmentVariableContext context = singleEnvironmentConfig.createEnvironmentContext();
        assertThat(context.getProperty(EnvironmentVariableContext.GO_ENVIRONMENT_NAME), is("One"));
    }

    @Test
    public void shouldReturnPipelineNamesContainedInIt() throws Exception {
        singleEnvironmentConfig.addPipeline(new CaseInsensitiveString("deployment"));
        singleEnvironmentConfig.addPipeline(new CaseInsensitiveString("testing"));
        List<CaseInsensitiveString> pipelineNames = singleEnvironmentConfig.getPipelineNames();
        assertThat(pipelineNames.size(), is(2));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("deployment")));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("testing")));
    }


    //TODO updates


}
