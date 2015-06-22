package com.thoughtworks.go.config;

import org.apache.commons.collections.map.SingletonMap;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by tomzo on 6/22/15.
 */
public class EnvironmentConfigBasicTest extends EnvironmentConfigBaseTest {
    @Before
    public void setUp() throws Exception {
        environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
    }

    @Test
    public void shouldUpdateName() {
        environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.NAME_FIELD, "PROD"));
        assertThat(environmentConfig.name(), is(new CaseInsensitiveString("PROD")));
    }
}
