package com.thoughtworks.go.config;

import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironmentVariable;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigConverterTest {

    private ConfigConverter configConverter;
    private GoCipher goCipher;

    @Before
    public void setUp() throws InvalidCipherTextException {
        goCipher = mock(GoCipher.class);
        configConverter = new ConfigConverter(goCipher);
        String encryptedText = "secret";
        when(goCipher.decrypt("encryptedvalue")).thenReturn(encryptedText);
    }

    @Test
    public void shouldConvertEnvironmentVariableWhenNotSecure()
    {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1","value");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.getValue(),is("value"));
        assertThat(result.getName(),is("key1"));
        assertThat(result.isSecure(),is(false));
    }

    @Test
    public void shouldConvertEnvironmentVariableWhenSecure()
    {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1",null,"encryptedvalue");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.isSecure(),is(true));
        assertThat(result.getValue(),is("secret"));
        assertThat(result.getName(),is("key1"));
    }

}
