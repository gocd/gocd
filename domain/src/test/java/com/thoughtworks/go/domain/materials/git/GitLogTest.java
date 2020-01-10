/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class GitLogTest {
    @Test
    void shouldConvertGitLogToModificationObject() {
        GitLog gitLog = new GitLog();
        gitLog.setSubject("Subject");
        gitLog.setDate("2009-02-11 17:26:36 -0800");
        gitLog.setAuthorEmail("bob@thoughtworks.com");
        gitLog.setAuthorName("Bob Ford");
        gitLog.setCommitHash("dklsjfhjksdhfjks34u283ryifdshfbs");
        gitLog.setRawBody("Something \n kjsdfsd \n");
        gitLog.setAdditionalInfo(new HashMap<>() {{
            put("Foo", "Bar");
        }});

        Modification modification = gitLog.toModification();

        assertThat(modification.getModifiedTime()).isEqualTo(DateUtils.parseISO8601("2009-02-11 17:26:36 -0800"));
        assertThat(modification.getUserName()).isEqualTo("Bob Ford");
        assertThat(modification.getEmailAddress()).isEqualTo("bob@thoughtworks.com");
        assertThat(modification.getRevision()).isEqualTo("dklsjfhjksdhfjks34u283ryifdshfbs");
        assertThat(modification.getComment()).isEqualTo("Something \n kjsdfsd");
        assertThat(modification.getAdditionalDataMap())
                .hasSize(2)
                .containsEntry("Foo", "Bar")
                .containsEntry("subject", "Subject");
    }
}
