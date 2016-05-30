package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class SCMConfigurationsTest {
    @Test
    public void shouldGetAllSCMsSortedByDisplayOrder() throws Exception {
        SCMConfiguration c1 = new SCMConfiguration("k1").with(SCMConfiguration.DISPLAY_ORDER, 2);
        SCMConfiguration c2 = new SCMConfiguration("k2").with(SCMConfiguration.DISPLAY_ORDER, 0);
        SCMConfiguration c3 = new SCMConfiguration("k3").with(SCMConfiguration.DISPLAY_ORDER, 1);
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(c1);
        scmConfigurations.add(c2);
        scmConfigurations.add(c3);

        List<SCMConfiguration> scmConfigurationList = scmConfigurations.list();

        assertThat(scmConfigurationList.get(0), is(c2));
        assertThat(scmConfigurationList.get(1), is(c3));
        assertThat(scmConfigurationList.get(2), is(c1));
    }

    @Test
    public void shouldConstructSCMConfiguration() throws Exception {
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("k1", "v1").with(Property.SECURE, Boolean.TRUE));

        SCMConfigurations scmConfigurations = new SCMConfigurations(scmPropertyConfiguration);

        assertThat(scmConfigurations.list().size(), is(1));
        SCMConfiguration scmConfiguration = scmConfigurations.list().get(0);
        assertThat(scmConfiguration.getKey(), is("k1"));
        assertThat(scmConfiguration.getValue(), is("v1"));
        assertThat(scmConfiguration.getOption(SCMConfiguration.REQUIRED), is(true));
        assertThat(scmConfiguration.getOption(SCMConfiguration.PART_OF_IDENTITY), is(true));
        assertThat(scmConfiguration.getOption(SCMConfiguration.SECURE), is(true));
        assertThat(scmConfiguration.getOption(SCMConfiguration.DISPLAY_NAME), is(""));
        assertThat(scmConfiguration.getOption(SCMConfiguration.DISPLAY_ORDER), is(0));
    }
}
