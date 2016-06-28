package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static com.thoughtworks.go.util.TestUtils.contains;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRApprovalTest extends CRBaseTest<CRApproval> {

    private final CRApproval manual;
    private final CRApproval success;
    private final CRApproval manualWithAuth;
    private final CRApproval badType;

    public CRApprovalTest()
    {
        manual = new CRApproval(CRApprovalCondition.manual);
        success = new CRApproval(CRApprovalCondition.success);

        manualWithAuth = new CRApproval(CRApprovalCondition.manual);
        manualWithAuth.setAuthorizedRoles(Arrays.asList("manager"));

        badType = new CRApproval();
    }

    @Override
    public void addGoodExamples(Map<String, CRApproval> examples) {
        examples.put("manual",manual);
        examples.put("success",success);
        examples.put("manualWithAuth",manualWithAuth);
    }

    @Override
    public void addBadExamples(Map<String, CRApproval> examples) {
        examples.put("invalidBadType",badType);
    }


    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "    \"type\": \"manual\",\n" +
                "      \"roles\": [\n" +
                "\n" +
                "      ],\n" +
                "      \"users\": [\n" +
                "\n\"joe\"" +
                "      ]\n" +
                "  }";
        CRApproval deserializedValue = gson.fromJson(json,CRApproval.class);

        assertThat(deserializedValue.getType(),is(CRApprovalCondition.manual));
        assertThat(deserializedValue.getAuthorizedUsers().isEmpty(),is(false));
        assertThat(deserializedValue.getAuthorizedRoles().isEmpty(),is(true));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }


    @Test
    public void shouldAppendPrettyLocationInErrors()
    {
        CRApproval a = new CRApproval();

        ErrorCollection errors = new ErrorCollection();
        a.getErrors(errors,"Pipeline abc");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("Pipeline abc; Approval"));
        assertThat(fullError,contains("Missing field 'type'"));
    }
}
