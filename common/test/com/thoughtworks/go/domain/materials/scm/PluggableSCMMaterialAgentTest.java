package com.thoughtworks.go.domain.materials.scm;

import com.google.gson.Gson;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluggableSCMMaterialAgentTest {
    @Mock
    private SCMExtension scmExtension;

    private ArgumentCaptor<SCMPropertyConfiguration> scmConfiguration;
    private ArgumentCaptor<SCMRevision> scmRevision;

    @Before
    public void setup() {
        initMocks(this);

        scmConfiguration = new ArgumentCaptor<SCMPropertyConfiguration>();
        scmRevision = new ArgumentCaptor<SCMRevision>();
    }

    @Test
    public void shouldTalkToPluginCheckoutForPrepare() {
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        pluggableSCMMaterial.setFolder("destination-folder");
        Modification modification = ModificationsMother.oneModifiedFile("r1");
        Map<String, String> additionalData = new HashMap<String, String>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        modification.setAdditionalData(new Gson().toJson(additionalData));
        MaterialRevision revision = new MaterialRevision(pluggableSCMMaterial, modification);
        PluggableSCMMaterialAgent pluggableSCMMaterialAgent = new PluggableSCMMaterialAgent(scmExtension, revision, new File("/tmp/pipelines/pipeline"));
        when(scmExtension.checkout(eq("pluginid"), scmConfiguration.capture(), eq("/tmp/pipelines/pipeline/destination-folder"), scmRevision.capture())).thenReturn(new Result());

        pluggableSCMMaterialAgent.prepare();

        verify(scmExtension).checkout(any(String.class), any(SCMPropertyConfiguration.class), any(String.class), any(SCMRevision.class));
        assertThat(scmConfiguration.getValue().size(), is(2));
        assertThat(scmConfiguration.getValue().get("k1").getValue(), is("v1"));
        assertThat(scmConfiguration.getValue().get("k2").getValue(), is("v2"));
        assertThat(scmRevision.getValue().getRevision(), is("r1"));
        assertThat(scmRevision.getValue().getTimestamp(), is(modification.getModifiedTime()));
        assertThat(scmRevision.getValue().getData().size(), is(2));
        assertThat(scmRevision.getValue().getDataFor("a1"), is("v1"));
        assertThat(scmRevision.getValue().getDataFor("a2"), is("v2"));
    }
}