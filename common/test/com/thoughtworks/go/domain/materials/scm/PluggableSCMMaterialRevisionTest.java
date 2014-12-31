package com.thoughtworks.go.domain.materials.scm;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class PluggableSCMMaterialRevisionTest {
    @Test
    public void shouldFindSCMMaterialRevisionEqual() {
        Date date = new Date();
        PluggableSCMMaterialRevision revisionOne = new PluggableSCMMaterialRevision("go-agent-12.1.0", date);
        PluggableSCMMaterialRevision revisionTwo = new PluggableSCMMaterialRevision("go-agent-12.1.0", date);
        PluggableSCMMaterialRevision revisionThree = new PluggableSCMMaterialRevision("go-agent-12.1.0", new Date());
        assertThat(revisionOne.equals(revisionTwo), is(true));
        assertThat(revisionOne.equals(revisionThree), is(false));
    }
}