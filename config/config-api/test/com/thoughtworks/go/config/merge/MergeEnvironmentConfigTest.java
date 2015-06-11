package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
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
import static org.junit.Assert.assertTrue;

public class MergeEnvironmentConfigTest {
    public MergeEnvironmentConfig singleEnvironmentConfig;
    public MergeEnvironmentConfig pairEnvironmentConfig;
    private static final String AGENT_UUID = "uuid";

    @Before
    public void setUp() throws Exception {
        singleEnvironmentConfig = new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("One")));
        pairEnvironmentConfig = new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("One")),
                new BasicEnvironmentConfig(new CaseInsensitiveString("One")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowPartsWithDifferentNames()
    {
        new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("One")),
                new BasicEnvironmentConfig(new CaseInsensitiveString("Two")));
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
        assertThat(singleEnvironmentConfig, Matchers.<EnvironmentConfig>is(another));
    }

    @Test
    public void twoEnvironmentConfigsShouldNotBeEqualIfnameNotEqual() throws Exception {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("other"));
        assertThat(another, Matchers.<EnvironmentConfig>is(Matchers.<EnvironmentConfig>not(singleEnvironmentConfig)));
    }
    @Test
    public void shouldAddEnvironmentVariablesToEnvironmentVariableContext() throws Exception {
        singleEnvironmentConfig.first().addEnvironmentVariable("variable-name", "variable-value");
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
        singleEnvironmentConfig.first().addPipeline(new CaseInsensitiveString("deployment"));
        singleEnvironmentConfig.first().addPipeline(new CaseInsensitiveString("testing"));
        List<CaseInsensitiveString> pipelineNames = singleEnvironmentConfig.getPipelineNames();
        assertThat(pipelineNames.size(), is(2));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("deployment")));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("testing")));
    }

    // merges

    @Test
    public void shouldReturnPipelineNamesFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
        pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("testing"));
        List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();
        assertThat(pipelineNames.size(), is(2));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("deployment")));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("testing")));
    }

    @Test
    public void shouldNotRepeatPipelineNamesFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
        pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("deployment"));
        List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("deployment")));
    }

    @Test
    public void shouldDeduplicateRepeatedPipelinesFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
        pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("deployment"));
        List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();
        assertThat(pipelineNames.size(), is(1));
        assertTrue(pairEnvironmentConfig.containsPipeline(new CaseInsensitiveString("deployment")));
    }

    @Test
    public void shouldHaveAgentsFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addAgent("123");
        pairEnvironmentConfig.get(1).addAgent("345");
        EnvironmentAgentsConfig agents = pairEnvironmentConfig.getAgents();

        assertTrue(pairEnvironmentConfig.hasAgent("123"));
        assertTrue(pairEnvironmentConfig.hasAgent("345"));
    }
    @Test
    public void shouldReturnAgentsUuidsFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addAgent("123");
        pairEnvironmentConfig.get(1).addAgent("345");
        EnvironmentAgentsConfig agents = pairEnvironmentConfig.getAgents();
        assertThat(agents.size(), is(2));
        assertThat(agents.getUuids(), hasItem("123"));
        assertThat(agents.getUuids(), hasItem("345"));
    }
    @Test
    public void shouldDeduplicateRepeatedAgentsFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addAgent("123");
        pairEnvironmentConfig.get(1).addAgent("123");
        EnvironmentAgentsConfig agents = pairEnvironmentConfig.getAgents();
        assertThat(agents.size(), is(1));
        assertThat(agents.getUuids(), hasItem("123"));
    }

    @Test
    public void shouldHaveVariablesFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name2", "variable-value2");

        assertTrue(pairEnvironmentConfig.hasVariable("variable-name1"));
        assertTrue(pairEnvironmentConfig.hasVariable("variable-name2"));
    }
    @Test
    public void shouldAddEnvironmentVariablesToEnvironmentVariableContextFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name2", "variable-value2");

        EnvironmentVariableContext context = pairEnvironmentConfig.createEnvironmentContext();
        assertThat(context.getProperty("variable-name1"), is("variable-value1"));
        assertThat(context.getProperty("variable-name2"), is("variable-value2"));
    }
    @Test
    public void shouldAddDeduplicatedEnvironmentVariablesToEnvironmentVariableContextFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name1", "variable-value1");

        assertThat(pairEnvironmentConfig.getVariables().size(), is(1));

        EnvironmentVariableContext context = pairEnvironmentConfig.createEnvironmentContext();
        assertThat(context.getProperty("variable-name1"), is("variable-value1"));
    }

    @Test
    public void shouldCreateErrorsForInconsistentEnvironmentVariables() throws Exception {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name1", "variable-value2");
        pairEnvironmentConfig.validate(null);
        assertThat(pairEnvironmentConfig.errors().isEmpty(), is(false));
        assertThat(pairEnvironmentConfig.errors().on(MergeEnvironmentConfig.CONSISTENT_KV),
                Matchers.is("Environment variable 'variable-name1' is defined more than once with different values"));
    }
    //TODO updates


}
