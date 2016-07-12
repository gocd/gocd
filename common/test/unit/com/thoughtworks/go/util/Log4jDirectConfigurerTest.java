/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import static org.hamcrest.core.Is.is;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Log4jDirectConfigurerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void teardown() {
        temporaryFolder.delete();
    }

    @Test
    public void shouldCreateNewFileWhenLog4jDoesNotExist() throws Exception {
        File file = new File(temporaryFolder.newFolder(), "anotherLog4jFile.properties");

        assertThat(file.exists(), is(false));

        Log4jDirectConfigurer log4jDirectConfigurer = new Log4jDirectConfigurer(file.getAbsolutePath());
        log4jDirectConfigurer.afterPropertiesSet();

        assertThat(file.exists(), is(true));
    }

    @Test
    public void shouldNotModifyTheExistingLog4jFile() throws Exception {
        File file = new File(temporaryFolder.newFolder(), "log4j-exist.properties");
        FileUtils.writeStringToFile(file, DEFAULT_LOG4J);

        assertThat(file.exists(), is(true));

        Log4jDirectConfigurer log4jDirectConfigurer = new Log4jDirectConfigurer(file.getAbsolutePath());
        log4jDirectConfigurer.afterPropertiesSet();

        assertThat(FileUtils.readFileToString(file), is(DEFAULT_LOG4J));
    }

    private static String DEFAULT_LOG4J = "log4j.rootLogger=INFO, FileAppender\n"
            + "log4j.logger.org.apache.velocity=ERROR\n";

}
