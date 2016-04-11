package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.JobResult;
import org.junit.Test;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AndCommandExecutorTest extends BuildSessionBasedTestCase {

    @Test
    public void emptyConditionShouldBePassed() {
        runBuild(and(), JobResult.Passed);
    }

    @Test
    public void singleConditionShouldBeResultOfTheCondition() {
        runBuild(and(noop()), JobResult.Passed);
        runBuild(and(fail()), JobResult.Failed);
    }

    @Test
    public void shouldBeFailedIfAnyConditionFailedInConditions() {
        runBuild(and(noop(), noop(), noop()), JobResult.Passed);
        runBuild(and(noop(), fail(), noop()), JobResult.Failed);
    }

    @Test
    public void shouldShortcutRestOfConditionsWhenConditionFailed() {
        runBuild(and(echo("1"), echo("2"), fail(), echo("3")), JobResult.Failed);
        assertThat(console.output(), is("1\n2"));
    }
}