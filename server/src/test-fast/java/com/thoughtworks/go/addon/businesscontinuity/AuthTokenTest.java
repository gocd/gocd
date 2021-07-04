package com.thoughtworks.go.addon.businesscontinuity;

import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthTokenTest {

    private SystemEnvironment systemEnvironment;
    @TempDir
    File configDir;
    private File tokenFile;

    @BeforeEach
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        tokenFile = new File(configDir, "business-continuity-token");
        when(systemEnvironment.configDir()).thenReturn(configDir);
    }

    @Test
    public void shouldValidateIfFileIsPresent() throws IOException {
        AuthToken authToken = new AuthToken(systemEnvironment);
        Assertions.assertThat(authToken.isValid()).isFalse();

        tokenFile.createNewFile();
        Assertions.assertThat(authToken.isValid()).isFalse();

        FileUtils.writeStringToFile(tokenFile, "blah", UTF_8);
        Assertions.assertThat(authToken.isValid()).isTrue();
    }

    @Test
    public void shouldGetTheTokenStringAfterStrippingWhitespace() throws IOException {
        AuthToken authToken = new AuthToken(systemEnvironment);

        FileUtils.writeStringToFile(tokenFile, "\n\t  bl = ah  \r\n  \t", UTF_8);
        Assertions.assertThat(authToken.toUsernamePassword()).isEqualTo(new UsernamePassword("bl", "ah"));
        Assertions.assertThat(authToken.forHttp()).isEqualTo("bl:ah");
    }
}
