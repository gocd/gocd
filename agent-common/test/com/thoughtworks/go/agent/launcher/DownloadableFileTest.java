/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadableFileTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldReturnTrueIfChecksumIsEqual() throws Exception {
        File inputFile = new File("test-resources/checksum.txt");
        assertTrue(DownloadableFile.matchChecksum(inputFile, "16508b3a80b828afd13318003b58626e"));
    }

    /*
     The checksum can be calculated from the command line with
     $ md5sum checksum.txt | cut -f1 -d\  | xxd -r -p | base64
    */
    @Test
    public void shouldReturnFalseIfChecksumIsNotEqual() throws Exception {
        File inputFile = new File("test-resources/checksum.txt");
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
        exception.expect(RuntimeException.class);
        exception.expectMessage("URL you provided to access Go Server: " + "invalidUrl" + " is not valid");
        DownloadableFile.AGENT.validatedUrl(serverUrlGenerator);
    }
}