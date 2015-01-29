package com.thoughtworks.go.domain.materials.scm;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class PluggableSCMMaterialRevisionTest {
    @Test
    public void shouldFindSCMMaterialRevisionEqual() {
        Date now = new Date();
        PluggableSCMMaterialRevision revisionOne = new PluggableSCMMaterialRevision("go-agent-12.1.0", now);
        PluggableSCMMaterialRevision revisionTwo = new PluggableSCMMaterialRevision("go-agent-12.1.0", now);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 5);
        Date later = calendar.getTime();
        PluggableSCMMaterialRevision revisionThree = new PluggableSCMMaterialRevision("go-agent-12.1.0", later);
        assertThat(revisionOne.equals(revisionTwo), is(true));
        assertThat(revisionOne.equals(revisionThree), is(false));
    }
}