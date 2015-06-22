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

public class MergeEnvironmentConfigTest extends EnvironmentConfigBaseTest {
    public MergeEnvironmentConfig singleEnvironmentConfig;
    public MergeEnvironmentConfig pairEnvironmentConfig;
    private static final String AGENT_UUID = "uuid";

    @Before
    public void setUp() throws Exception {
        singleEnvironmentConfig = new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("UAT")));
        pairEnvironmentConfig = new MergeEnvironmentConfig(
                new BasicEnvironmentConfig(new CaseInsensitiveString("UAT")),
                new BasicEnvironmentConfig(new CaseInsensitiveString("UAT")));

        super.environmentConfig = pairEnvironmentConfig;
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowPartsWithDifferentNames()
    {
        new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("UAT")),
                new BasicEnvironmentConfig(new CaseInsensitiveString("Two")));
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
