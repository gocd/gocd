package com.thoughtworks.go.domain.cctray.viewers;

import org.junit.Test;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AllowedViewersTest {
    @Test
    public void shouldCheckViewPermissionsInACaseInsensitiveWay() throws Exception {
        AllowedViewers viewers = new AllowedViewers(s("USER1", "user2", "User3", "AnoTherUsEr"));

        assertThat(viewers.contains("user1"), is(true));
        assertThat(viewers.contains("USER1"), is(true));
        assertThat(viewers.contains("User1"), is(true));
        assertThat(viewers.contains("USER2"), is(true));
        assertThat(viewers.contains("uSEr3"), is(true));
        assertThat(viewers.contains("anotheruser"), is(true));
        assertThat(viewers.contains("NON-EXISTENT-USER"), is(false));
    }
}