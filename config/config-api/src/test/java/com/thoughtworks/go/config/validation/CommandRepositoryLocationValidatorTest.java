/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.thoughtworks.go.util.SystemEnvironment.COMMAND_REPOSITORY_DIRECTORY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandRepositoryLocationValidatorTest {

    private CommandRepositoryLocationValidator validator;
    private CruiseConfig cruiseConfig;
    private ServerConfig serverConfig;
    private String repoRootLocation;

    @Before public void setUp() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        validator = new CommandRepositoryLocationValidator(systemEnvironment);
        cruiseConfig = mock(BasicCruiseConfig.class);
        serverConfig = mock(ServerConfig.class);
        when(cruiseConfig.server()).thenReturn(serverConfig);
        when(systemEnvironment.get(COMMAND_REPOSITORY_DIRECTORY)).thenReturn("db/task_repository");
        repoRootLocation = new File("db/task_repository").getAbsolutePath();
    }

    @Test
    public void shouldNotAllowEmptyValueForTaskRepositoryLocation() throws Exception {
        assertValidationFailedWith("", "Command Repository Location cannot be empty");
    }

    @Test
    public void shouldNotAllowSpacesForTaskRepositoryLocation() throws Exception {
        assertValidationFailedWith("          ", "Command Repository Location cannot be empty");
    }

    @Test
    public void shouldNotAllowToSpecifyPathOutsideTaskRepository() {
        String expectedMessage = String.format("Invalid  Repository Location, repository should be a subdirectory under %s", repoRootLocation);
        assertValidationFailedWith(".", expectedMessage);
        assertValidationFailedWith("../folder", expectedMessage);
    }

    @Test
    public void shouldAllowTaskRepositoryPath() throws Exception {
        assertValidationPassed("./test/sub");
        assertValidationPassed("../task_repository/test");
    }

    @Test
    public void shouldNotAllowTaskRepoPathThatContainingSpecialSysbols() {
        String message = "Invalid Repository Location";
        assertValidationFailedWith("/var/lib", message);
        assertValidationFailedWith("\\var/lib", message);
        assertValidationFailedWith("~/foo", message);
        assertValidationFailedWith("c:\\", message);
        assertValidationFailedWith("d:/", message);
    }


    private void assertValidationPassed(final String repoLocation) throws Exception {
        when(serverConfig.getCommandRepositoryLocation()).thenReturn(repoLocation);
        validator.validate(cruiseConfig);
    }

    private void assertValidationFailedWith(final String repoLocation, final String message) {
        when(serverConfig.getCommandRepositoryLocation()).thenReturn(repoLocation);
        try {
            validator.validate(cruiseConfig);
            fail("should have thrown :" + message);
        } catch (Exception e) {
            assertThat(e.getMessage(), is(message));
        }
    }
}
