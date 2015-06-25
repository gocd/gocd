package com.thoughtworks.go.config.preprocessor;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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