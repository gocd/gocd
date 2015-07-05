package com.thoughtworks.go.plugin.access.configrepo.migration;

import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironmentVariable;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRDependencyMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRPackageMaterial;
import com.thoughtworks.go.plugin.configrepo.CREnvironment_1;
import com.thoughtworks.go.plugin.configrepo.material.CRDependencyMaterial_1;
import com.thoughtworks.go.plugin.configrepo.material.CRPackageMaterial_1;
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

    @Test
    public void shouldMigrateDependencyMaterial()
    {
        CRDependencyMaterial_1 dependencyMaterial_1 = new CRDependencyMaterial_1("pipe1","pipelineA","test");
        CRMaterial result = migration.migrate(dependencyMaterial_1);
        assertThat(result.getName(),is("pipe1"));
        assertThat(result instanceof CRDependencyMaterial,is(true));
        CRDependencyMaterial crDependencyMaterial = (CRDependencyMaterial)result;
        assertThat(crDependencyMaterial.getPipelineName(),is("pipelineA"));
        assertThat(crDependencyMaterial.getStageName(),is("test"));
    }

    @Test
    public void shouldMigratePackageMaterial()
    {
        CRPackageMaterial_1 packageMaterial = new CRPackageMaterial_1("myapt", "apt-package-plugin-id");
        CRMaterial result = migration.migrate(packageMaterial);
        assertThat(result.getName(),is("myapt"));
        assertThat(result instanceof CRPackageMaterial,is(true));
        CRPackageMaterial crDependencyMaterial = (CRPackageMaterial)result;
        assertThat(crDependencyMaterial.getPackageId(),is("apt-package-plugin-id"));
    }
}
