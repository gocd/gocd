package com.thoughtworks.go.plugin.infra;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PluginValidatorTest {
    private PluginValidator pluginValidator = new PluginValidator();

    @Test
    public void shouldReturnTrueIfFilenameExtensionIsJar() {
        assertTrue(pluginValidator.namecheckForJar("valid_name.jar"));
    }

    @Test
    public void shouldReturnFalseIfFilenameExtensionIsNotJar() {
        assertFalse(pluginValidator.namecheckForJar("invalid_name.png"));
        assertFalse(pluginValidator.namecheckForJar("invalid_name_without_extension"));
    }
}
