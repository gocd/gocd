package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.JobResult;
import org.junit.Test;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class OrCommandExecutorTest extends BuildSessionBasedTestCase {

    @Test
    public void emptyConditionShouldBeFailed() {
        runBuild(or(), JobResult.Failed);
    }

    @Test
    public void singleConditionShouldBeResultOfTheCondition() {
        runBuild(or(noop()), JobResult.Passed);
        runBuild(or(fail()), JobResult.Failed);
    }

    @Test
    public void shouldBePassedIfAnyConditionPassedInConditions() {
        runBuild(or(noop(), noop(), noop()), JobResult.Passed);
        runBuild(or(fail(), fail(), noop()), JobResult.Passed);
        runBuild(or(fail(), fail(), fail()), JobResult.Failed);
    }

    @Test
    public void shouldShortcutRestOfConditionsWhenConditionPassed() {
        runBuild(or(echo("1"), echo("2"), echo("3")), JobResult.Passed);
        assertThat(console.output(), is("1"));
    }

}