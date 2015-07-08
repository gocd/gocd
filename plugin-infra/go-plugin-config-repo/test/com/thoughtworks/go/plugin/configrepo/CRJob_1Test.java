package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.plugin.configrepo.tasks.BuildTask_1Test;
import com.thoughtworks.go.plugin.configrepo.tasks.CRBuildTask_1;
import com.thoughtworks.go.plugin.configrepo.tasks.CRTask_1;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRJob_1Test extends CRBaseTest<CRJob_1> {

    private final CRBuildTask_1 rakeTask = CRBuildTask_1.rake();
    private final CRBuildTask_1 antTask = CRBuildTask_1.ant();

    private final CRJob_1 buildRake;
    private final CRJob_1 build2Rakes;
    private final CRJob_1 jobWithVar;
    private final CRJob_1 jobWithResource;
    private final CRJob_1 jobWithTab;
    private final CRJob_1 jobWithProp;
    private final CRJob_1 invalidJobNoName;

    public CRJob_1Test()
    {
        buildRake = new CRJob_1("build", rakeTask);
        build2Rakes = new CRJob_1("build", rakeTask,CRBuildTask_1.rake("Rakefile.rb","compile"));

        jobWithVar = new CRJob_1("build", rakeTask);
        jobWithVar.addEnvironmentVariable("key1","value1");

        jobWithResource = new CRJob_1("test",antTask);
        jobWithResource.addResource("linux");

        jobWithTab = new CRJob_1("test",antTask);
        jobWithTab.addTab(new CRTab_1("test","results.xml"));

        jobWithProp = new CRJob_1("perfTest",rakeTask);
        jobWithProp.addProperty(new CRPropertyGenerator_1("perf","test.xml","substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"));

        invalidJobNoName = new CRJob_1();
    }

    @Override
    public void addGoodExamples(Map<String, CRJob_1> examples) {
        examples.put("buildRake",buildRake);
        examples.put("build2Rakes",build2Rakes);
        examples.put("jobWithVar",jobWithVar);
        examples.put("jobWithResource",jobWithResource);
        examples.put("jobWithTab",jobWithTab);
        examples.put("jobWithProp",jobWithProp);
    }

    @Override
    public void addBadExamples(Map<String, CRJob_1> examples) {
        examples.put("invalidJobNoName",invalidJobNoName);
    }


    @Test
    public void shouldHandlePolymorphismWhenDeserializingTasks()
    {
        String json = gson.toJson(buildRake);

        CRJob_1 deserializedValue = (CRJob_1)gson.fromJson(json,CRJob_1.class);

        assertThat(deserializedValue.getTasks().get(0) instanceof CRBuildTask_1,is(true));
    }
}
