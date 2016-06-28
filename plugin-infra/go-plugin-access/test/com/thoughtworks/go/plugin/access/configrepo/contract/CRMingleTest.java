package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRMingleTest extends CRBaseTest<CRMingle> {

    private final CRMingle mingle;
    private final CRMingle invalidNoUrl;
    private final CRMingle invalidNoId;

    public CRMingleTest()
    {
        mingle = new CRMingle("http://mingle.example.com","my_project");
        invalidNoUrl = new CRMingle(null,"my_project");
        invalidNoId = new CRMingle("http://mingle.example.com",null);

    }

    @Override
    public void addGoodExamples(Map<String, CRMingle> examples) {
        examples.put("mingle",mingle);
    }

    @Override
    public void addBadExamples(Map<String, CRMingle> examples) {
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
        CRMingle deserializedValue = gson.fromJson(json,CRMingle.class);

        assertThat(deserializedValue.getBaseUrl(),is("https://mingle.example.com"));
        assertThat(deserializedValue.getProjectIdentifier(),is("foobar_widgets"));
        assertThat(deserializedValue.getMqlGroupingConditions(),is("status > 'In Dev'"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
