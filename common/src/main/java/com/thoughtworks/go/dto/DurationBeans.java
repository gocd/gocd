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
package com.thoughtworks.go.dto;

import java.util.Arrays;

import com.thoughtworks.go.domain.BaseCollection;

public class DurationBeans extends BaseCollection<DurationBean> {
    public DurationBeans(DurationBean... durationBeans) {
        addAll(Arrays.asList(durationBeans));
    }

    public DurationBean byId(long id) {
        for (DurationBean durationBean : this) {
            if (durationBean.getJobId().equals(id)) {
                return durationBean;
            }
        }
        return new DurationBean(id);
    }
}
