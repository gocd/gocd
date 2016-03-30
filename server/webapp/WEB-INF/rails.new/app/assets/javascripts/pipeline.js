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

/* Pipeline Observer: check the pipeline status in specific interval. */
var PipelineObserver = Class.create();

PipelineObserver.prototype = {
    initialize : function(keyOfListObjects) {
        this.isFirstRun = true;
        this.dropExpiredCallback = true;
        if (!keyOfListObjects) {
            this.keyOfListObjects = 'pipelines';
        } else {
            this.keyOfListObjects = keyOfListObjects;
        }
    },
    notify : function(json) {
        window.last_transfered_json = json;

        if (this.isFirstRun) {
            this.onPageLoad();
        }

        this.checkIfNewPipelineHistoryArrived();

        if(window.pipelineSelector){
            $('build-pipelines').innerHTML = this.getTemplate().process({ data : pipelineSelector.filter(json) });
        } else {
            $('build-pipelines').innerHTML = this.getTemplate().process({ data : json });
        }

        if (window.paginator) {
            this.renderPagination(json);
        }
    },
    getTemplate: function(){
        if(!this._Template){
            this._Template = TrimPath.parseDOMTemplate('pipeline-list-template');
        }
        return this._Template;
    },
    onPageLoad: function() {
        if (this.keyOfListObjects == 'pipelines') {
            //Do not collapse all pipelines when page load, make user know how many build plans is there
            pipelinePage.initializeCollapsedStageArrayWhenNeeded();
            //this.collapseAllPipelines('pipelines');
        } else if (this.keyOfListObjects == 'history') {
            this.rememberTopStageHistoryLabel();
            this.collapseAllStageHistoryButTheFirst();
        }
    },
    checkIfNewPipelineHistoryArrived: function() {
        var history = window.last_transfered_json[this.keyOfListObjects];
        if (history && history.length > 0 && history[0].current_label != this.top_pipeline_history_label) {
            this.collapseAllStageHistoryButTheFirst();
            this.rememberTopStageHistoryLabel();
        }
    },
    rememberTopStageHistoryLabel: function() {
        var history = window.last_transfered_json[this.keyOfListObjects];
        if (history && history.length > 0) {
            this.top_pipeline_history_label = history[0].current_label;
        }
    },
    collapseAllPipelines: function(key) {
        pipelinePage.initializeCollapsedStageArrayWhenNeeded($A(window.last_transfered_json[key]).collect(function(
                pipelineJson) {
            return pipelineJson['stages'].collect(function(stageJson) {
                return stageJson.uniqueStageId;
            })
        }).flatten());
        this.isFirstRun = false;
    },
    collapseAllStageHistoryButTheFirst: function(key) {
        pipelinePage.initializeCollapsedStageArrayWhenNeeded($A(window.last_transfered_json[this.keyOfListObjects]).collect(function(
                stage) {
            return stage.uniqueStageId;
        }));

        pipelinePage.initializeCollapsedStageArrayWhenNeeded(pipelinePage.collapsedStageArray.without(pipelinePage.collapsedStageArray.first()));

        this.isFirstRun = false;
    },
    renderPagination: function(json) {
        paginator.setParametersFromJson(json);
        $('page_links').innerHTML = $('page-links-template').value.process({pagination: json});
    }
};

var BuildCause = Class.create({
    initialize: function(buildCauseKey) {
        this._buildCauseKey = buildCauseKey;
    },
    _linkElementId: function(id){
        return this._buildCauseKey + '-' + id + '-buildCause'; 
    },
    hideOrShowBuildCause: function(id) {
        ExclusivePopup.create(this._linkElementId(id)).toggle();
    },
    getBuildCauseClass: function(id) {
        return ExclusivePopup.getStatus(this._linkElementId(id));
    },
    getStyleText: function(id){
        return ExclusivePopup.getStyleText(this._linkElementId(id));
    },
    getPositionClass: function(id) {
        return ExclusivePopup.getPositionClass(this._linkElementId(id));
    }
});

var PipelinePage = Class.create({
    initialize: function() {
        this.buildCauseActor = new BuildCause("stage");
    },
    isPipelineScheduleButtonEnabled: function(pipeline) {
        return !pipelineActions.shouldShowPipelineScheduleButtonAsSpinner(pipeline) && pipeline.canForce == 'true';
    },
    shouldShowPipelinePauseButtonAsSpinner: function(pipeline) {
        return pipelineActions.spinningPauseButton.include(pipeline.name);
    },
    shouldShowStageCancelButtonAsSpinner: function(stageId){
        return pipelineActions.spinningCancelButton.include(stageId);
    },
    isScheduleButtonEnabled: function(stageStatus) {
        if (!this._scheduleButtonEnableStatus) {
            this._scheduleButtonEnableStatus = $A(['passed', 'failed', 'cancelled', 'unknown']);
        }

        return this._scheduleButtonEnableStatus.include(stageStatus);
    },
    isCancelButtonEnabled: function(stageStatus) {
        if (!this._cancelButtonEnabledStatus) {
            this._cancelButtonEnabledStatus = $A(['building', 'scheduled', 'assigned', 'preparing', 'completing', 'failing']);
        }

        return this._cancelButtonEnabledStatus.include(stageStatus);
    },
    isPipelinePaused: function(pipeline) {
        return pipeline.paused == 'true';
    },
    canPause: function(pipeline) {
        return pipeline.canPause == 'true';
    },
    pauseStatusText: function(pipeline) {
        var text = '';
        if (this.isPipelinePaused(pipeline)) {
            text = 'Scheduling is paused';
            if (pipeline.pauseBy) text += ' by ' + pipeline.pauseBy;
            if (pipeline.pauseCause) text += ' (' + pipeline.pauseCause + ')';
        }
        return text;
    },
    ifShowAgentInBuildStatusMessage: function(buildStatus) {
        if (!this._showAgentInStatusMessageStatus) {
            this._showAgentInStatusMessageStatus = ['building', 'preparing', 'completing'];
        }

        for (var index = 0; index < this._showAgentInStatusMessageStatus.length; index++) {
            if (this._showAgentInStatusMessageStatus[index] == buildStatus) {
                return true;
            }
        }
        return false;
    },
    getFirstStageOfPipeline: function(pipeline) {
        //return false if no stages
        if (!pipeline.stages || pipeline.stages.length < 1) return false;
        return pipeline.stages[0];
    },
    initializeCollapsedStageArrayWhenNeeded: function(array) {
        // This array is to store the hidden pipelines; it will be still hidden when re-rendered
        if (!this.collapsedStageArray || array) {
            this.collapsedStageArray = $A(array);
        }
    },
    isStageCollapsed: function(stageUniqueId) {
        if (this.collapsedStageArray && this.collapsedStageArray.include(stageUniqueId)) {
            return true;
        }

        return false;
    },
    toggleStagePanel: function(uniqueStageId) {
        var pipelineElement = $('pipeline-' + uniqueStageId);
        var pipelineCollapseButton = $(uniqueStageId + '-collapse-link');

        if (pipelineCollapseButton.hasClassName('collapsed')) {
            pipelineCollapseButton.removeClassName('collapsed').addClassName('expanded');
            pipelineElement.removeClassName('closed');

            this.collapsedStageArray = this.collapsedStageArray.without(uniqueStageId);
        } else {
            pipelineCollapseButton.addClassName('collapsed').removeClassName('expanded');

            pipelineElement.addClassName('closed');

            this.collapsedStageArray.push(uniqueStageId)
            this.collapsedStageArray = this.collapsedStageArray.uniq();
        }
    },
    collapseAllStagePanels: function() {
        var last_json = window.last_transfered_json;
        var ITERATIVE_OBJECTS_KEY = 'pipelines';
        var INNER_ITERATIVE_OBJECTS_KEY = 'stages';

        this.initializeCollapsedStageArrayWhenNeeded($A(last_json[ITERATIVE_OBJECTS_KEY]).collect(function(
                pipelineJson) {
            return pipelineJson[INNER_ITERATIVE_OBJECTS_KEY].pluck('uniqueStageId')
        }).flatten());

        this._applyStageChangesOnAllStageElements(this._collapseStageElement);
    },
    expandAllStagePanels: function() {
        this.initializeCollapsedStageArrayWhenNeeded([]);

        this._applyStageChangesOnAllStageElements(this._expandStageElement);
    },
    _applyStageChangesOnAllStageElements: function(changeFunction) {
        var last_json = window.last_transfered_json;
        var ITERATIVE_OBJECTS_KEY = 'pipelines';
        var INNER_ITERATIVE_OBJECTS_KEY = 'stages';

        var pipelinePage = this;

        if (last_json && last_json[ITERATIVE_OBJECTS_KEY] && last_json[ITERATIVE_OBJECTS_KEY].length > 0) {
            $A(last_json[ITERATIVE_OBJECTS_KEY]).each(function(pipelineJson) {
                pipelineJson[INNER_ITERATIVE_OBJECTS_KEY].collect(changeFunction.bind(pipelinePage));
            });
        }
    },
    _expandStageElement: function(stageJson) {
        this._changeStateOnStageElement(stageJson['uniqueStageId'], 'collapsed', 'expanded', Element.removeClassName);
    },
    _collapseStageElement: function(stageJson) {
        this._changeStateOnStageElement(stageJson['uniqueStageId'], 'expanded', 'collapsed', Element.addClassName);
    },
    _changeStateOnStageElement: function(stageId, fromState, toState, toggleElement) {
        var stageElement = $('pipeline-' + stageId);
        var stageCollapseButton = stageElement.down('.collapse-or-expand-button', 0);
        stageCollapseButton.removeClassName(fromState).addClassName(toState);
        toggleElement(stageElement, 'closed');
    },
    collapseAllPipelineHistoryPanels: function() {
        var last_json = window.last_transfered_json;
        this.initializeCollapsedStageArrayWhenNeeded($A(window.last_transfered_json.history).collect(function(
                pipeline) {
            return pipeline.uniqueStageId;
        }));

        if (last_json && last_json.history && last_json.history.length > 0) {
            $A(last_json.history).each(function(pipeline) {
                var pipelineElement = $('pipeline-' + pipeline.uniqueStageId);
                var pipelineCollapseButton = pipelineElement.down('.collapse-or-expand-button', 0);

                pipelineCollapseButton.addClassName('collapsed').removeClassName('expanded');
                pipelineElement.addClassName('closed');
            });
        }
    },
    expandAllPipelineHistoryPanels: function() {
        var last_json = window.last_transfered_json;
        this.initializeCollapsedStageArrayWhenNeeded([]);

        if (last_json && last_json.history && last_json.history.length > 0) {
            $A(last_json.history).each(function(pipeline) {
                var pipelineElement = $('pipeline-' + pipeline.uniqueStageId);
                var pipelineCollapseButton = pipelineElement.down('.collapse-or-expand-button', 0);
                pipelineCollapseButton.removeClassName('collapsed').addClassName('expanded');
                pipelineElement.removeClassName('closed');
            });
        }
    },
    switchToPage: function(pipelineName, stageName, pageNumber) {
        var start = (pageNumber - 1) * paginator.perPage;
        var url = contextPath + "/stageHistory.json?pipelineName="
                + pipelineName + "&stageName=" + stageName + "&start=" + start;
        dashboard_periodical_executer.setUrl(url);
        dashboard_periodical_executer.fireNow();
    },
    fixIEZIndexBugs: function(zindex_seed) {
        if (zindex_seed) {
            this._ie_zindex_seed = zindex_seed;
        }

        if (Prototype.Browser.IE) {
            return 'z-index: ' + this._ie_zindex_seed--;
        }
    }
});


var PromptValid = Class.create();

PromptValid.prototype = {
    initialize : function() {},

    valid : function(cause) {
        if (cause == undefined || cause == null || cause == '') {
            return {result: true};
        }

        if (cause.length > 255) {
            return {result: false, reason: 'Keep it simple - reason can only be 255 characters or less!'};
        }

        if (/^[a-zA-Z0-9_\-\|\.\s,]*$/.test(cause)) {
            return {result: true};
        } else {
            return {result: false, reason: 'Invalid character. Please use a-z, A-Z, 0-9, fullstop, underscore, hyphen and pipe:'};
        }
    }
}

var RememberButtonSpinningModule = {
    rememberButtonIsSpinning: function(type, id){
        this._getButtonSpinningArray(type).push(id);
        this.stopSpinningButton.bind(this).delay(6, type, id);
    },
    stopSpinningButton: function(type, id){
        this['spinning' + type + 'Button'] = this._getButtonSpinningArray(type).without(id);
    },
    _getButtonSpinningArray: function(type){
        if(!this['spinning' + type + 'Button']){
            this['spinning' + type + 'Button'] = $A();
        }
        return this['spinning' + type + 'Button'];
    }
}


var PipelineActions = Class.create({
    initialize : function() {
        this.promptValid = new PromptValid();
        this.spinningScheduleButton = $A();
        this.spinningPauseButton = $A();
        this.spinningCancelButton = $A();
    },
    cancelPipeline: function(stageId, link) {
        if(this.spinningCancelButton.include(stageId)){
            return;/* Can't duplicate submit when submitting */
        }

        var url = contextPath + "/cancel.json?id=" + stageId;
        var confirmed = confirm("This will cancel all active jobs in this stage. Are you sure?");
        if (confirmed) {
            if(link) {
                $(link).removeClassName('cancel-build-link').addClassName('submiting-link');
                $(link).onclick = null;
            }
            this.rememberButtonIsSpinning('Cancel', stageId);

            new Ajax.Request(url, {
                method: 'put',
                requestHeaders: {
                    Confirm: 'true'
                },
                onComplete: function() {
                    dashboard_periodical_executer.fireNow();
                }
            });
        }
    },
    _schedulePipelineTemplate: function(pipelineName, link, toggleCssFunction) {
        if(this.spinningScheduleButton.include(pipelineName)){
            return;/* Can't duplicate submit when submitting */
        }

        var url = contextPath + "/api/pipelines/" + pipelineName + "/schedule";

        toggleCssFunction($(link));
        $(link).onclick = null;

        this.rememberButtonIsSpinning('Schedule', pipelineName);

        new Ajax.Request(url, {
            method: 'post',
            requestHeaders: {
                Confirm: 'true'
            },
            onComplete: function() {
                dashboard_periodical_executer.fireNow();
            },
            on406: function(transport) {
                var json = transport.responseText.evalJSON();
                if (json.error) {
                    FlashMessageLauncher.error('Error while scheduling build', json.error);
                }
            }
        });
    },
    schedulePipeline: function(pipelineName, link){
        this._schedulePipelineTemplate(pipelineName, link, function(link){
            link.removeClassName('schedule-build-link-enabled').addClassName('submiting-link');
        });
    },
    schedulePipelineInHistoryPage: function(pipelineName, link){
        this._schedulePipelineTemplate(pipelineName, link, function(link){
            link.removeClassName('force-run-pipeline').addClassName('submiting-force-run-pipeline');
        });
    },
    pausePipeline: function(pipelineName, linkElement) {
        if(this.spinningPauseButton.include(pipelineName)){
            return;/* Can't duplicate submit when submitting */
        }

        var url = contextPath + "/api/pipelines/" + pipelineName + "/pause";

        var cause = prompt("Specify the reason why you want to stop scheduling on this pipeline (only a-z, A-Z, 0-9, fullstop, underscore, hyphen and pipe is valid) :");
        while (!this.isPauseReasonValid(cause).result) {
            cause = prompt(this.isPauseReasonValid(cause).reason);
        }
        if (cause != null) {

            this.rememberButtonIsSpinning('Pause', pipelineName);

            if($(linkElement)){
                linkElement.addClassName('submiting-link');
            }

            new Ajax.Request(url, {
                method: 'post',
                parameters : 'pauseCause=' + cause,
                requestHeaders: {
                    Confirm: 'true'
                },
                onComplete: function() {
                    dashboard_periodical_executer.fireNow();
                }
            });
        }
    },
    isPauseReasonValid: function(cause) {
        return this.promptValid.valid(cause);
    },
    unpausePipeline: function(pipelineName, linkElement) {
        if(this.spinningPauseButton.include(pipelineName)){
            return;/* Can't duplicate submit when submitting */
        }

        $('pause-' + pipelineName).removeClassName('pause-build-link').addClassName('submiting-link');

        var url = contextPath + "/api/pipelines/" + pipelineName + "/unpause";

        this.rememberButtonIsSpinning('Pause', pipelineName);

        if($(linkElement)){
            linkElement.addClassName('submiting-link');
        }
        
        new Ajax.Request(url, {
            method: 'post',
            requestHeaders: {
                Confirm: 'true'
            },
            onComplete: function() {
                dashboard_periodical_executer.fireNow();
            }
        });
    },
    shouldShowPipelineScheduleButtonAsSpinner: function(pipeline) {
        if(this.spinningScheduleButton.include(pipeline.name)){
            return true;
        }

        return pipeline.forcedBuild == 'true';
    }
}, RememberButtonSpinningModule);

var StageActions = Class.create({
    initialize : function() {
        this.spinningRerunButton = $A();
    },
    runStage: function(pipelineName, pipelineLabel, stageName, link) {
        if(this.spinningRerunButton && this.spinningRerunButton.include(pipelineName + pipelineLabel + stageName)){
            return;/* Can't duplicate submit when submitting */
        }

        var confirmed = confirm("Do you want to run the stage '" + stageName +"'?");

        if (confirmed) {
            this.rememberButtonIsSpinning('Rerun', pipelineName + pipelineLabel + stageName);

            if(link){
                $(link).removeClassName('rerun').addClassName('submiting-rerun');
            }

            var url = contextPath + "/run/" + pipelineName + "/" + pipelineLabel + "/" + stageName;
            new Ajax.Request(url, {
                method: 'post',
                requestHeaders: {
                    Confirm: 'true'
                },
                onComplete: function() {
                    dashboard_periodical_executer.fireNow();
                },
                on401: function(transport) {
                    alert("Not authorized to approve this stage.");
                },
                on406: function(transport) {
                    var json = transport.responseText.evalJSON();
                    if (json.error) {
                        FlashMessageLauncher.error('Failed to rerun stage', json.error);
                    }
                }
            });
        }
    }
}, RememberButtonSpinningModule);