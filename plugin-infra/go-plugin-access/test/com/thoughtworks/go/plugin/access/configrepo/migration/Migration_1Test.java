package com.thoughtworks.go.plugin.access.configrepo.migration;

import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironmentVariable;
import com.thoughtworks.go.plugin.configrepo.CREnvironment_1;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class Migration_1Test {
    private Migration_1 migration;

    @Before
    public void SetUp()
    {
        migration = new Migration_1();
    }

    @Test
    public void shouldMigrateEnvironmentName()
    {
        CREnvironment result = migration.migrate(new CREnvironment_1("dev"));
        assertThat(result.getName(),is("dev"));
    }

    @Test
    public void shouldMigrateEnvironmentVariablesInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addEnvironmentVariable("key1","value1");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getEnvironmentVariables(),hasItem(new CREnvironmentVariable("key1","value1")));
    }

    @Test
    public void shouldMigrateAgentsInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addAgent("123");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getAgents(),hasItem("123"));
    }

    @Test
    public void shouldMigratePipelinesInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addPipeline("pipe1");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getPipelines(),hasItem("pipe1"));
    }
}
