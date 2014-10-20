/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.domain.user;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.domain.PersistentObject;

public class PipelineSelections extends PersistentObject implements Serializable {

    private Date lastUpdate;
    public static final PipelineSelections ALL = new PipelineSelections() {
        @Override public boolean includesGroup(PipelineConfigs group) {
            return true;
        }

        @Override public boolean includesPipeline(PipelineConfig pipeline) {
            return true;
        }
    };
    private List<String> pipelines;
    private Long userId;
    private List<CaseInsensitiveString> caseInsensitivePipelineList = new ArrayList<CaseInsensitiveString>();

    public PipelineSelections() {
        this(new ArrayList<String>());
    }

    public PipelineSelections(List<String> unselectedPipelines) {
        this(unselectedPipelines, new Date(), null);
    }

    public PipelineSelections(List<String> unselectedPipelines, Date date, Long userId) {
        update(unselectedPipelines, date, userId);
    }

    public Date lastUpdated() {
        return lastUpdate;
    }

    public void update(List<String> unselectedPipelines, Date date, Long userId) {
        this.userId = userId;
        this.setSelections(ListUtil.join(unselectedPipelines, ","));
        this.lastUpdate = date;
    }

    public boolean includesGroup(PipelineConfigs group) {
        for (PipelineConfig pipelineConfig : group) {
            if (!includesPipeline(pipelineConfig)) {
                return false;
            }
        }
        return true;
    }

    public boolean includesPipeline(PipelineConfig pipeline) {
        return includesPipeline(CaseInsensitiveString.str(pipeline.name()));
    }

    public boolean includesPipeline(String pipelineName) {
        return !caseInsensitivePipelineList().contains(new CaseInsensitiveString(pipelineName));
    }

    private List<String> pipelineList() {
        return pipelines;
    }

    private List<CaseInsensitiveString> caseInsensitivePipelineList() {
        return caseInsensitivePipelineList;
    }


    public String getSelections() {
        return ListUtil.join(pipelineList(), ",");
    }

    public void setSelections(String unselectedPipelines) {
        this.pipelines = ListUtil.split(unselectedPipelines, ",");
        List<CaseInsensitiveString> pipelineList = new ArrayList<CaseInsensitiveString>();
        for (String pipeline : pipelines) {
            pipelineList.add(new CaseInsensitiveString(pipeline));
        }
        this.caseInsensitivePipelineList = pipelineList;
    }

    public static PipelineSelections singleSelection(final String pipelineName) {
        return new PipelineSelections() {
            @Override public boolean includesPipeline(PipelineConfig pipeline) {
                return compare(pipelineName, CaseInsensitiveString.str(pipeline.name()));
            }

            @Override public boolean includesPipeline(String pipeline) {
                return compare(pipelineName, pipeline);
            }

            @Override public boolean includesGroup(PipelineConfigs group) {
                return true;
            }

            private boolean compare(String pipelineName, String name) {
                return name.equalsIgnoreCase(pipelineName);
            }
        };
    }

    public Long userId() {
        return userId;
    }

}
