/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.agent.launcher;

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadableFileTest {
    @Test
    public void shouldReturnTrueIfChecksumIsEqual() throws Exception {
        File inputFile = new File("src/test/resources/checksum.txt");
        assertTrue(DownloadableFile.matchChecksum(inputFile, "16508b3a80b828afd13318003b58626e"));
    }

    /*
     The checksum can be calculated from the command line with
     $ md5sum checksum.txt | cut -f1 -d\  | xxd -r -p | base64
    */
    @Test
    public void shouldReturnFalseIfChecksumIsNotEqual() throws Exception {
        File inputFile = new File("src/test/resources/checksum.txt");
        assertFalse(DownloadableFile.matchChecksum(inputFile, "nonmat"));
    }

    @Test
    public void shouldCheckIfFileExists() throws Exception {
        assertTrue(DownloadableFile.AGENT.doesNotExist());
    }

    @Test
    public void shouldValidateTheUrl() throws Exception {
        ServerUrlGenerator serverUrlGenerator = ServerUrlGeneratorMother.generatorFor("localhost", 9090);
        assertThat(DownloadableFile.AGENT.validatedUrl(serverUrlGenerator), is("http://localhost:9090/go/admin/agent"));
    }

    @Test
    public void shouldThrowExceptionIfUrlIsInvalid() throws Exception {
        ServerUrlGenerator serverUrlGenerator = mock(ServerUrlGenerator.class);
        when(serverUrlGenerator.serverUrlFor("admin/agent")).thenReturn("invalidUrl");
        assertThatThrownBy(() -> DownloadableFile.AGENT.validatedUrl(serverUrlGenerator))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("URL you provided to access Go Server: " + "invalidUrl" + " is not valid");
    }
}