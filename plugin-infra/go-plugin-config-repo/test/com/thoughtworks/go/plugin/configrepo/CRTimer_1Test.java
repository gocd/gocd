package com.thoughtworks.go.plugin.configrepo;

import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRTimer_1Test extends CRBaseTest<CRTimer_1> {

    private final CRTimer_1 timer;
    private final CRTimer_1 invalidNoTimerSpec;

    public CRTimer_1Test()
    {
        timer = new CRTimer_1("0 15 10 * * ? *");

        invalidNoTimerSpec = new CRTimer_1();
    }

    @Override
    public void addGoodExamples(Map<String, CRTimer_1> examples) {
        examples.put("timer",timer);
    }

    @Override
    public void addBadExamples(Map<String, CRTimer_1> examples) {
        examples.put("invalidNoTimerSpec",invalidNoTimerSpec);
    }

    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "    \"spec\": \"0 0 22 ? * MON-FRI\",\n" +
                "    \"only_on_changes\": true\n" +
                "  }";
        CRTimer_1 deserializedValue = gson.fromJson(json,CRTimer_1.class);

        assertThat(deserializedValue.getTimerSpec(),is("0 0 22 ? * MON-FRI"));
        assertThat(deserializedValue.isOnlyOnChanges(),is(true));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
