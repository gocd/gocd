package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.JobResult;
import org.junit.Test;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CondCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test(expected = IllegalArgumentException.class)
    public void needAtLeastTwoSubcommands() {
        cond(noop());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noConditionIsInvalid() {
        cond();
    }

    @Test
    // if(test) { action }
    public void ifCondition() {
        runBuild(cond(noop(), echo("foo")), JobResult.Passed);
        assertThat(console.output(), is("foo"));
        console.clear();
        runBuild(cond(fail(), echo("foo")), JobResult.Passed);
        assertThat(console.output(), is(""));

        runBuild(cond(noop(), fail()), JobResult.Failed);
        runBuild(cond(fail(), noop()), JobResult.Passed);
        runBuild(cond(fail(), fail()), JobResult.Passed);
    }

    @Test
    // if (test) { action1 } else { action2 }
    public void ifElseCondition() {
        runBuild(cond(noop(), echo("foo"), echo("bar")), JobResult.Passed);
        assertThat(console.output(), is("foo"));

        console.clear();
        runBuild(cond(fail(), echo("foo"), echo("bar")), JobResult.Passed);
        assertThat(console.output(), is("bar"));

        runBuild(cond(noop(), fail(), noop()), JobResult.Failed);
        runBuild(cond(fail(), fail(), noop()), JobResult.Passed);
    }

    @Test
    // if (test1) { action1 } elseif (test2) { action2 }
    public void multipleConditionBranches() {
        runBuild(cond(
                fail(), echo("1"),
                fail(), echo("2"),
                noop(), echo("3"),
                noop(), echo("4")), JobResult.Passed);
        assertThat(console.output(), is("3"));
    }

    @Test
    // if (test1) { action1 } elseif (test2) { action2 } else { action3 }
    public void multipleConditionWithElseBranch() {
        runBuild(cond(
                fail(), echo("1"),
                fail(), echo("2"),
                echo("3")), JobResult.Passed);
        assertThat(console.output(), is("3"));
    }

}