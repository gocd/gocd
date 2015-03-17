package com.thoughtworks.go.domain.cctray.viewers;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class NoViewersTest {
    @Test
    public void shouldSayItContainsNoUsersAlways() throws Exception {
        Viewers noViewers = NoViewers.INSTANCE;

        assertThat(noViewers.contains("abc"), is(false));
        assertThat(noViewers.contains("def"), is(false));
        assertThat(noViewers.contains(null), is(false));
        assertThat(noViewers.contains(""), is(false));
    }
}