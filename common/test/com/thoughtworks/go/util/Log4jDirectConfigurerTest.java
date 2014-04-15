/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
import org.junit.Test;

public class Log4jDirectConfigurerTest {
    private File file;
    private File log4jExist;


    @Before
    public void setup() {
        file = new File("anotherLog4jFile.properties");
        log4jExist = new File("log4j-exist.properties");
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(file);
        FileUtils.deleteQuietly(log4jExist);
    }

    @Test
    public void shouldCreateNewFileWhenLog4jDoesNotExist() throws Exception {
        assertThat(file.exists(), is(false));
        Log4jDirectConfigurer log4jDirectConfigurer = new Log4jDirectConfigurer(file.getName());
        log4jDirectConfigurer.afterPropertiesSet();
        assertThat(file.exists(), is(true));
    }

    @Test
    public void shouldNotModifyTheExistingLog4jFile() throws Exception {
        createLog4J();
        Log4jDirectConfigurer log4jDirectConfigurer = new Log4jDirectConfigurer(log4jExist.getName());
        log4jDirectConfigurer.afterPropertiesSet();
        assertThat(FileUtils.readFileToString(log4jExist), is(DEFAULT_LOG4J));
    }

    private static String DEFAULT_LOG4J = "log4j.rootLogger=INFO, FileAppender\n"
            + "log4j.logger.org.apache.velocity=ERROR\n";

    private void createLog4J() throws IOException {
        FileUtils.writeStringToFile(log4jExist, DEFAULT_LOG4J);
    }
}
