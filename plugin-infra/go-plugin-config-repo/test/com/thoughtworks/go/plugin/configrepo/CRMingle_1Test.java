package com.thoughtworks.go.plugin.configrepo;

import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRMingle_1Test extends CRBaseTest<CRMingle_1> {

    private final CRMingle_1 mingle;
    private final CRMingle_1 invalidNoUrl;
    private final CRMingle_1 invalidNoId;

    public CRMingle_1Test()
    {
        mingle = new CRMingle_1("http://mingle.example.com","my_project");
        invalidNoUrl = new CRMingle_1(null,"my_project");
        invalidNoId = new CRMingle_1("http://mingle.example.com",null);

    }

    @Override
    public void addGoodExamples(Map<String, CRMingle_1> examples) {
        examples.put("mingle",mingle);
    }

    @Override
    public void addBadExamples(Map<String, CRMingle_1> examples) {
        examples.put("invalidNoUrl",invalidNoUrl);
        examples.put("invalidNoId",invalidNoId);
    }

    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "    \"base_url\": \"https://mingle.example.com\",\n" +
                "    \"project_identifier\": \"foobar_widgets\",\n" +
                "    \"mql_grouping_conditions\": \"status > 'In Dev'\"\n" +
                "  }";
        CRMingle_1 deserializedValue = gson.fromJson(json,CRMingle_1.class);

        assertThat(deserializedValue.getBaseUrl(),is("https://mingle.example.com"));
        assertThat(deserializedValue.getProjectIdentifier(),is("foobar_widgets"));
        assertThat(deserializedValue.getMqlGroupingConditions(),is("status > 'In Dev'"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
