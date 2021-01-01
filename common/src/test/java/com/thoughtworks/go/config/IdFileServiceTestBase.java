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
package com.thoughtworks.go.config;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public abstract class IdFileServiceTestBase {
    protected IdFileService idFileService;
    protected String DATA = "data";

    @After
    public void tearDown() {
        idFileService.delete();
    }

    @Test
    public void shouldLoadDataFromFile() throws Exception {
        assertThat(idFileService.load(), is(DATA));
    }

    @Test
    public void shouldStoreDataToFile() throws Exception {
        idFileService.store("some-id");

        assertThat(idFileService.load(), is("some-id"));
    }

    @Test
    public void shouldCheckIfDataPresent() throws Exception {
        assertTrue(idFileService.dataPresent());

        idFileService.delete();
        assertFalse(idFileService.dataPresent());


        idFileService.store("");
        assertFalse(idFileService.dataPresent());
    }

    @Test
    public void shouldDeleteFile() throws Exception {
        assertTrue(idFileService.file.exists());

        idFileService.delete();

        assertFalse(idFileService.file.exists());
    }
}