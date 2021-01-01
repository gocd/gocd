/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.domain.valuestreammap;

import java.util.Date;

import com.thoughtworks.go.domain.materials.Modification;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

public class SCMRevisionTest {
    @Test
    public void shouldGetModificationDetails() throws Exception {
        Date dateTime = new Date();
        Modification modification=new Modification("user","comment","email", dateTime,"r1");
        SCMRevision scmRevision=new SCMRevision(modification);
        Assert.assertThat(scmRevision.getComment(),is("comment"));
        Assert.assertThat(scmRevision.getUser(),is("user"));
        Assert.assertThat(scmRevision.getRevisionString(),is("r1"));
        Assert.assertThat(scmRevision.getModifiedTime(),is(dateTime));
    }

    @Test
    public void shouldRenderUsernameForDisplay() throws Exception {
        Modification modification=new Modification(null,"comment","email", new Date(), "r1");
        SCMRevision scmRevision=new SCMRevision(modification);
        Assert.assertThat(scmRevision.getUser(), is(modification.getUserDisplayName()));
    }
}
