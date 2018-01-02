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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.ClassMockery;
import static org.hamcrest.core.Is.is;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class PipelineActiveCheckerTest {
    private ClassMockery mockery;
    private CurrentActivityService service;
    private PipelineActiveChecker checker;
    private PipelineIdentifier pipelineIdentifier;

    @Before
    public void setUp() throws Exception {
        mockery = new ClassMockery();
        service = mockery.mock(CurrentActivityService.class);
        pipelineIdentifier = new PipelineIdentifier("cruise", 1, "label-1");
        checker = new PipelineActiveChecker(service, pipelineIdentifier);
    }

    @Test
    public void shouldFailIfPipelineIsActive() {
        mockery.checking(new Expectations() {
            {
                one(service).isAnyStageActive(pipelineIdentifier);
                will(returnValue(true));
            }
        });

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState().isSuccess(), is(false));
        assertThat(result.getServerHealthState().getDescription(),
                is("Pipeline[name='cruise', counter='1', label='label-1'] is still in progress"));
    }

    @Test
    public void shouldPassIfPipelineIsNotActive() {
        mockery.checking(new Expectations() {
            {
                one(service).isAnyStageActive(pipelineIdentifier);
                will(returnValue(false));
            }
        });

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState().isSuccess(), is(true));
    }
}
