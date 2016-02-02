/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.websocket;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.helper.ModificationsMother;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JsonMessageTest {
    @Test
    public void encodeDecodeMaterialRevisions() {
        MaterialRevisions revisions = ModificationsMother.createHgMaterialRevisions();
        Modification latestModification = new Modification("user2", "comment2", "email2", new Date(), "122cf27f16eadc362733328dd481d8a2c29915e1");
        latestModification.createModifiedFile("file", "folder", ModifiedAction.added);

        revisions.getRevisions().get(0).getModifications().add(0, latestModification);
        String encode = JsonMessage.encode(new Message(Action.ping, revisions));
        Message decode = JsonMessage.decode(encode);
        MaterialRevisions data = (MaterialRevisions) decode.getData();
        MaterialRevision revision = data.getRevisions().get(0);
        assertThat(revision.getRevision(), is(revisions.getRevisions().get(0).getRevision()));
        assertThat(revision.getModifications().size(), is(1));
    }
}
