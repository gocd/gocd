/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.domain.user;

import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;

public class PipelineSelectionsHelper {
    public static PipelineSelections with(List<String> pipelines, Date date, Long userId, boolean blacklist) {
        List<CaseInsensitiveString> params = CaseInsensitiveString.list(pipelines);
        DashboardFilter filter = blacklist ? new BlacklistFilter(DEFAULT_NAME, params, new HashSet<>()) : new WhitelistFilter(DEFAULT_NAME, params, new HashSet<>());
        return new PipelineSelections(new Filters(Collections.singletonList(filter)), date, userId);
    }

    public static PipelineSelections with(List<String> pipelines) {
        return with(pipelines, new Date(), null, true);
    }
}
