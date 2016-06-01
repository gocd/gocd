package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRTimer;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRTimerTest extends CRBaseTest<CRTimer> {

    private final CRTimer timer;
    private final CRTimer invalidNoTimerSpec;

    public CRTimerTest()
    {
        timer = new CRTimer("0 15 10 * * ? *");

        invalidNoTimerSpec = new CRTimer();
    }

    @Override
    public void addGoodExamples(Map<String, CRTimer> examples) {
        examples.put("timer",timer);
    }

    @Override
    public void addBadExamples(Map<String, CRTimer> examples) {
        examples.put("invalidNoTimerSpec",invalidNoTimerSpec);
    }

    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "    \"spec\": \"0 0 22 ? * MON-FRI\",\n" +
                "    \"only_on_changes\": true\n" +
                "  }";
        CRTimer deserializedValue = gson.fromJson(json,CRTimer.class);

        assertThat(deserializedValue.getTimerSpec(),is("0 0 22 ? * MON-FRI"));
        assertThat(deserializedValue.isOnlyOnChanges(),is(true));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
