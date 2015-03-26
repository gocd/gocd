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

package com.thoughtworks.go.domain;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

public class PipelineIdentifierTest {

    @Test
    public void shouldUseCounterIfItExists() {
        PipelineIdentifier identifier = new PipelineIdentifier("cruise", 1, "label-1");
        assertThat(identifier.pipelineLocator(), is("cruise/1"));
    }

    @Test
    @Ignore("will fix this in some time")
    public void shouldUseLabelIfNoCounter() {
        try {
            new PipelineIdentifier("cruise", null, "label-1");
            fail("Allowed creation of pipeline identifier without counter");
        }
        catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline Identifier cannot be created without a counter"));
        }
    }

    @Test
    public void shouldUseLabelForDisplay() {
        PipelineIdentifier identifier = new PipelineIdentifier("cruise", 1, "label-1");
        assertThat(identifier.pipelineLocatorForDisplay(), is("cruise/label-1"));
    }

    @Test
    public void shouldReturnURN() throws Exception {
        PipelineIdentifier identifier = new PipelineIdentifier("cruise", 1, "label-1");
        assertThat(identifier.asURN(), is("urn:x-go.studios.thoughtworks.com:job-id:cruise:1"));
    }
}
