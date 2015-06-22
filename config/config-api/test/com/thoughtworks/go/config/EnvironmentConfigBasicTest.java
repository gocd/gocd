package com.thoughtworks.go.config;

import org.junit.Before;

/**
 * Created by tomzo on 6/22/15.
 */
public class EnvironmentConfigBasicTest extends EnvironmentConfigBaseTest {
    @Before
    public void setUp() throws Exception {
        environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
    }
}
