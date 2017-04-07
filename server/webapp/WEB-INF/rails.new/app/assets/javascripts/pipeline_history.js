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
 *************************GO-LICENSE-END**********************************/

var PipelineHistoryObserver = Class.create();

PipelineHistoryObserver.prototype = {
    initialize : function(keyOfListObjects) {
    },
    notify : function(json) {
        var myModifiers = {
            escapeQuotes: function(str) {
                return str.replace(/"/g, '&quot;');
            },
        }
        $('pipeline-history').innerHTML = this.getTemplate().process(
            { data : json, _MODIFIERS: myModifiers});
        if (window.paginator) {
            this.renderPagination(json);
        }
    },
    getTemplate: function() {
        if (!this._template) {
            this._template = TrimPath.parseDOMTemplate('pipeline-history-list-template');
        }
        return this._template;
    },
    renderPagination: function(json) {
        paginator.setParametersFromJson(json);
        $('page_links').innerHTML = $('page-links-template').value.process({pagination: json});
    }
};

var PipelineHistoryPage = Class.create();

PipelineHistoryPage.prototype = {
    initialize : function() {
        this.buildCauseActor = new BuildCause("pipeline");
    },
    switchToPage: function(pipelineName, pageNumber) {
        var start = (pageNumber - 1) * paginator.perPage;
        var url = contextPath + "/pipelineHistory.json?pipelineName=" + pipelineName + "&start=" + start;
        dashboard_periodical_executor.setUrl(url);
        dashboard_periodical_executor.fireNow();
    },
    findLastStageNameFromConfiguration: function(stageConfigs) {
        this.lastStageName = stageConfigs[stageConfigs.length - 1].stageName;
    },
    isLastGate: function(currentStageConfig) {
        return currentStageConfig.stageName == this.lastStageName;
    },
    shouldShowGate: function(currentStage, allStages, configs) {
        if (!this._isLastStage(currentStage, allStages)) return true;
        if (!this._configContains(currentStage, configs)) return false;
        return currentStage.stageName != this.lastStageName;
    },
    _isLastStage: function(currentStage, allStages) {
        return $A(allStages).last().stageName == currentStage.stageName
    },
    _configContains: function(stage, configs) {
        var _contains = false
        $A(configs).each(function(config) {
            if (_contains) return;
            if (stage.stageName == config.stageName) {
                _contains = true;
            }
        });
        return _contains;
    },
    _gateClass: function(approvedBy) {
        if (approvedBy == 'cruise') {
            return "gate-completed-auto";
        } else {
            return "gate-completed-manual";
        }
    },
    getCompletedGateClass: function(stage) {
        return this._gateClass(stage.approvedBy);
    },

    isPipelineScheduleButtonEnabled: function(pipeline_history) {
        return 'true' == pipeline_history.canForce && !this.shouldShowPipelineScheduleButtonAsSpinner(pipeline_history);
    },
    shouldShowPipelineScheduleButtonAsSpinner: function(pipelineName) {
        if (pipelineActions.spinningScheduleButton.include(pipelineName)) {
            return true;
        }

        return pipelineName.forcedBuild && pipelineName.forcedBuild == 'true';
    },
    shouldShowPipelinePauseButtonAsSpinner: function(pipelineName) {
        if (pipelineActions.spinningPauseButton.include(pipelineName)) {
            return true;
        }

        return false;
    },
    isPipelinePaused: function(pipeline_history) {
        return pipeline_history.paused == 'true';
    },
    canPause: function(pipeline_history) {
        return pipeline_history.canPause == 'true';
    },
    pauseStatusText: function(pipeline_history) {
        var text = '';
        if (this.isPipelinePaused(pipeline_history)) {
            text = 'Scheduling is paused';
            if (pipeline_history.pauseBy) text += ' by ' + pipeline_history.pauseBy;
            if (pipeline_history.pauseCause) text += ' (' + pipeline_history.pauseCause + ')';
        }
        return text;
    },
    fixIEZIndexBugs: function(zindex_seed) {
        if (zindex_seed) {
            this._ie_zindex_seed = zindex_seed;
        }

        if (Prototype.Browser.IE) {
            return 'z-index: ' + this._ie_zindex_seed--;
        }
    },
    noNextStagesRunning: function(pipelines, stageName) {
        var size = pipelines.length;
        for (var i = 0; i < size; i++) {
            var stage = this._getNextStageByName(pipelines[i], stageName);
            if (stage != null && stage.stageStatus == 'Building') {
                return false;
            }
        }
        return true;
    },
    _getNextStageByName: function(pipeline, stageName) {
        var stages = $A(pipeline.stages)
        var size = stages.size()
        for (var i = 0; i < size; i++) {
            if (stages[i].stageName == stageName) {
                return stages[i + 1];
            }
        }
        return null;
    },
    getApprovalType: function(config, stageIndex) {
        return config.stages[stageIndex].isAutoApproved == 'true' ? "auto" : "manual";
    },
    getState: function(stageStatus){
        return stageStatus.toLowerCase();
    },
    fixLayout: function(){
        var max_width = 0;
        $$('table').each(function(table){
            if(Element.getWidth(table) > max_width){
                max_width = Element.getWidth(table);
            }
        });

        if (jQuery('#bd').width() < max_width + 30) {
                jQuery('#bd').css({minWidth: max_width + 30 + 'px'});
        }
    }
}
