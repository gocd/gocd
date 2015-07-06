package com.thoughtworks.go.server.resources;

import com.google.gson.Gson;
import com.thoughtworks.go.server.config.ConfigurableSSLSettings;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.fail;

public class SslConfigValidationTest {
    @Test
    public void SSLConfigFileShouldExist() throws Exception {
        String filePath = new SystemEnvironment().get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH);
        File actualConfigFile = new File(getClass().getResource(filePath).toURI());
        String config = FileUtils.readFileToString(actualConfigFile);

        try {
            new Gson().fromJson(config, ConfigurableSSLSettings.Config.class);
        } catch (Exception e) {
            fail("Invalid content in ssl.config file. " + e.getMessage());
        }
    }
}
