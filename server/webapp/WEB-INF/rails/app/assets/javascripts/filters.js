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

/* This file is for pipeline filters */
var PipelineSelector = Class.create({
    initialize: function(){
        this.cookie = new PipelineSelectorCookie();
    },
    persistHiddenPipelineNames: function(){
        this.cookie.persist(this.pipelineNames);
    },
    _isPipelineHidden: function(pipelineName) {
        return this.cookie.isPipelineHidden(pipelineName);
    },
    _showPipeline: function(pipelineName) {
        this.cookie.showPipeline(pipelineName);
    },
    _showPipelines: function(pipelineNames) {
        this.cookie.showPipelines(pipelineNames);
    },
    _hidePipeline: function(pipelineName) {
        this.cookie.hidePipeline(pipelineName);
    },
    _hidePipelines: function(pipelineNames) {
        this.cookie.hidePipelines(pipelineNames);
    },
    _hasNoHiddenPipeline: function() {
        return this.cookie.hasNoHiddenPipeline(this.pipelineNames.length);
    },
    _hasNoShownPipeline: function() {
        return this.cookie.hasNoShownPipeline(this.pipelineNames.length);
    },
    _pipelineNamesInGroup: function(group) {
        return this._pipelineNames(this._pipelineObjsInGroup(group));
    },
    getTemplate: function(){
        if(!this._Template){
            this._Template = TrimPath.parseDOMTemplate('pipeline-selector-template');
        }
        return this._Template;
    },
    renderPipelineSelectorBar: function(){
        $('pipeline-selector-content').innerHTML = this.getTemplate().process({pipelineNames: this.pipelineNames, groups: this.groups});
    },
    shouldPipelineBeChecked: function(name){
        return !this._isPipelineHidden(name);
    },
    shouldGroupBeChecked : function(group) {
        var pipelineNames = this._pipelineNamesInGroup(group);
        if(pipelineNames.length == 0) return false;

        for (var i = 0; i < pipelineNames.length; i++) {
            if (this._isPipelineHidden(pipelineNames[i])) {
                return false;
            }
        }
        return true;
    },
    filter: function(json){
        var allPipelineNames = $A();
        var groups = $H();
        //filter the pipelines
        if (json.pipelines && json.pipelines.length > 0) {
            for (var pipe_index = 0; pipe_index < json.pipelines.length; pipe_index++) {
                var pipeline = json.pipelines[pipe_index];
                if (groups.get(pipeline.group)) {
                    groups.get(pipeline.group).push(pipeline.name);
                } else {
                    var names = $A();
                    groups.set(pipeline.group, names);
                    names.push(pipeline.name);
                }
                allPipelineNames.push(pipeline.name);
                if (this._isPipelineHidden(pipeline.name)) {
                    pipeline.hide_in_ui = true;
                }
            }
        }
        //collect the pipeline names
        if(this.pipelineNames){
            this.oldPipelineNames = this.pipelineNames;
        }
        this.groups = groups;
        this.pipelineNames = allPipelineNames;
        //post actions of filter, render the selector bar if needed
        this.postFilter();

        return json;
    },
    postFilter: function(){
        //modification detect
        //if(this.oldPipelineNames && this.pipelineNames.length != this.oldPipelineNames.length){
            this.renderPipelineSelectorBar();
        //}
    },
    togglePipeline: function(pipelineName, groupName){
        this.showSpinner();

        if(this._isPipelineHidden(pipelineName)){
            this._showPipeline(pipelineName);
        } else {
            this._hidePipeline(pipelineName);
        }

        this.persistHiddenPipelineNames();
        this._checkGroup(groupName);
        this.checkAll();
    },
    _checkGroup: function(groupName){
        var groupCheckboxObj = $('pipeline-selector-of-' + groupName);
        if(!groupCheckboxObj) return;

        var pipelineObjs = this._pipelineObjsInGroup(groupName);
        for(var i = 0; i < pipelineObjs.length; i++) {
            if(!pipelineObjs[i].checked) {
                groupCheckboxObj.checked = '';
                return;
            }
        }
        groupCheckboxObj.checked = 'checked';
    },
    toggleGroup: function(groupCheckboxObj, group) {
        var pipelineObjs = this._pipelineObjsInGroup(group);
        if (groupCheckboxObj.checked) {
            // hide -> show
            this._showPipelineObjs(pipelineObjs);
        } else {
            // show -> hide
            this._hidePipelineObjs(pipelineObjs);
        }
    },
    _pipelineObjsInGroup : function(group) {
        var groupElement  = $('pipelines-of-' + group);
        if (!groupElement) return $A();
        return groupElement.select('input');
    },
    showAll: function(){
         return this._hasNoHiddenPipeline();
    },
    showNone: function(){
        return this._hasNoShownPipeline();
    },
    _allPipelines: function() {
        return $$('#pipeline-selector-container input');
    },
    _pipelineNames : function(inputs) {
        var names =  $A();
        inputs.each(function(input){
            names.push(input.id.sub('pipeline-selector-of-', ""));
        });
        return names;
    },
    toggleAll: function(){
        $('pipelines-selector-none').checked = '';
        this._showPipelineObjs(this._allPipelines());
    },
    _showPipelineObjs: function(pipelineObjs) {
        this.showSpinner();

        pipelineObjs.each(function(li) {
            $(li).checked = "checked";
        });

        var shouldShow = this._pipelineNames(pipelineObjs);
        this._showPipelines(shouldShow);
        this.persistHiddenPipelineNames();
    },
    toggleNone: function() {
        $('pipelines-selector-all').checked = '';
        this._hidePipelineObjs($$('#pipeline-selector-container input'));
    },
    _hidePipelineObjs: function(pipelineObjs) {
        this.showSpinner();

        pipelineObjs.each(function(li) {
            $(li).checked = "";
        });

        var shouldHide = this._pipelineNames(pipelineObjs);
        this._hidePipelines(shouldHide);
        this.persistHiddenPipelineNames();

    },
    checkAll: function(){
        if(this.showAll()){
            $('pipelines-selector-all').checked = 'checked';
        } else {
            $('pipelines-selector-all').checked = '';
        }

        if(this.showNone()){
            $('pipelines-selector-none').checked = 'checked';
        } else {
            $('pipelines-selector-none').checked = '';
        }
    },
    showSpinner: function(){
        $('pipeline-selector-spinner').show();
    },
    hideSpinner: function(){
        $('pipeline-selector-spinner').hide();
    }
})