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
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

@Getter
@Setter
public class GitLog {
    private String subject;
    private String commitHash;
    private String authorName;
    private String authorEmail;
    private String date;
    private String rawBody;
    private HashMap<String, String> additionalInfo;

    public GitLog() {
    }

    Modification toModification() {
        Modification modification = new Modification(
                StringUtils.stripToNull(authorName),
                StringUtils.stripToNull(rawBody),
                StringUtils.stripToNull(authorEmail),
                DateUtils.parseISO8601(date),
                StringUtils.stripToNull(commitHash));

        additionalInfo.put("subject", subject);
        modification.setAdditionalData(additionalInfo);
        return modification;
    }
}
