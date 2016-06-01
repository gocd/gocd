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
import org.apache.commons.lang.builder.ToStringBuilder;

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
    private List<CaseInsensitiveString> caseInsensitivePipelineList = new ArrayList<>();
    private boolean isBlacklist;

    public PipelineSelections() {
        this(new ArrayList<String>());
    }

    public PipelineSelections(List<String> unselectedPipelines) {
        this(unselectedPipelines, new Date(), null, true);
    }

    public PipelineSelections(List<String> unselectedPipelines, Date date, Long userId, boolean isBlacklist) {
        update(unselectedPipelines, date, userId, isBlacklist);
    }

    public Date lastUpdated() {
        return lastUpdate;
    }

    public void update(List<String> selections, Date date, Long userId, boolean isBlacklist) {
        this.userId = userId;
        this.isBlacklist = isBlacklist;
        updateSelections(selections);
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
        boolean isInCurrentSelection = caseInsensitivePipelineList().contains(new CaseInsensitiveString(pipelineName));

        if (isBlacklist) {
            return !isInCurrentSelection;
        }
        return isInCurrentSelection;
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

    private void setSelections(String unselectedPipelines) {
        this.pipelines = ListUtil.split(unselectedPipelines, ",");
        List<CaseInsensitiveString> pipelineList = new ArrayList<>();
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

    public boolean isBlacklist() {
        return isBlacklist;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public PipelineSelections addPipelineToSelections(CaseInsensitiveString pipelineToAdd) {
        ArrayList<String> updatedListOfPipelines = new ArrayList<>();
        updatedListOfPipelines.addAll(pipelines);
        updatedListOfPipelines.add(CaseInsensitiveString.str(pipelineToAdd));

        this.updateSelections(updatedListOfPipelines);
        return this;
    }

    private void updateSelections(List<String> selections) {
        this.setSelections(ListUtil.join(selections, ","));
    }
}
