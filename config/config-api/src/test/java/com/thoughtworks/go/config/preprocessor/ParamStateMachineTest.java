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
package com.thoughtworks.go.config.preprocessor;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ParamStateMachineTest {

    private ParamHandler handler;

    @Before
    public void setUp() throws Exception {
        handler = mock(ParamHandler.class);
    }

    @Test
    public void shouldClearPatternWhenFound() throws Exception {
        ParamStateMachine stateMachine = new ParamStateMachine();
        stateMachine.process("#{pattern}", handler);

        assertThat(ParamStateMachine.ReaderState.IN_PATTERN.pattern.length(), is(0));
        verify(handler).handlePatternFound(any(StringBuilder.class));
    }

    @Test
    public void shouldClearPatternWhenParameterCannotBeResolved() throws Exception {
        ParamStateMachine stateMachine = new ParamStateMachine();
        doThrow(new IllegalStateException()).when(handler).handlePatternFound(any(StringBuilder.class));

        try {
            stateMachine.process("#{pattern}", handler);
        } catch (Exception e) {
            //Ignore to assert on the pattern
        }
        assertThat(ParamStateMachine.ReaderState.IN_PATTERN.pattern.length(), is(0));
        verify(handler).handlePatternFound(any(StringBuilder.class));
    }
}
