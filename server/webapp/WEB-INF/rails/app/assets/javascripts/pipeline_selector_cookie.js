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

var Blacklist = Class.create({
    initialize: function(pipelineNames){
        if(pipelineNames) {
            this.hiddenPipelineNames = pipelineNames;
        } else {
            this._restoreHiddenPipelineNames();
        }
    },
    _restoreHiddenPipelineNames: function(){
        var name_array_string = getCookie('hidden-pipeline-names');
        if(name_array_string && !name_array_string.blank()){
            this.hiddenPipelineNames = $A(name_array_string.split(',')).invoke('strip');
        } else {
            this.hiddenPipelineNames = $A();
        }
    },
    persist: function() {
        setCookie('pipeline-selector-strategy', 'blacklist');
        setCookie('hidden-pipeline-names', this.hiddenPipelineNames.join(','));
    },
    _cleanCookie: function(){
        deleteCookie('hidden-pipeline-names');
    },
    isPipelineHidden: function(pipelineName) {
        return this.hiddenPipelineNames.include(pipelineName);
    },
    showPipeline: function(pipelineName) {
        this.hiddenPipelineNames = this.hiddenPipelineNames.without(pipelineName);
    },
    showPipelines: function(pipelineNames) {
        var shouldHide = $A();
        for(var i = 0; i < this.hiddenPipelineNames.length; i++) {
            if(!pipelineNames.include(this.hiddenPipelineNames[i])){
                shouldHide.push(this.hiddenPipelineNames[i]);
            }
        }
        this.hiddenPipelineNames = shouldHide;
    },
    hidePipeline: function(pipelineName) {
        this.hiddenPipelineNames.push(pipelineName);
    },
    hidePipelines: function(pipelineNames) {
        for(var i = 0; i < pipelineNames.length; i++) {
            this.hidePipeline(pipelineNames[i]);
        }
    },
    hasNoHiddenPipeline: function() {
        return this.hiddenPipelineNames.length == 0;
    },
    hasNoShownPipeline: function(totalPipelinesCount) {
        return this.hiddenPipelineNames.length == totalPipelinesCount;
    },
    pipelinesCount: function() {
        return this.hiddenPipelineNames.length;
    },
    inverse: function(allPipelineNames) {
        this._cleanCookie();
        return new Whitelist(arrayMinus(allPipelineNames, this.hiddenPipelineNames));
    },
    toString: function() {
        return 'blacklist';
    }
});

function arrayMinus(array1, array2) {
    var result = $A();
    for(var i = 0; i < array1.length; i++) {
        if(!array2.include(array1[i])) {
            result.push(array1[i]);
        }
    }
    return result;
}

var Whitelist = Class.create({
    initialize: function(pipelineNames){
        if(pipelineNames) {
            this.shownPipelineNames = pipelineNames;
        } else {
            this._restoreShownPipelineNames();
        }
    },
    _restoreShownPipelineNames: function(){
        var name_array_string = getCookie('shown-pipeline-names');
        if(name_array_string && !name_array_string.blank()){
            this.shownPipelineNames = $A(name_array_string.split(',')).invoke('strip');
        } else {
            this.shownPipelineNames = $A();
        }
    },
    persist: function() {
        setCookie('pipeline-selector-strategy', 'whitelist');
        setCookie('shown-pipeline-names', this.shownPipelineNames.join(','));
    },
    _cleanCookie: function(){
        deleteCookie('shown-pipeline-names');
    },
    isPipelineHidden: function(pipelineName) {
        return !this.shownPipelineNames.include(pipelineName);
    },
    showPipeline: function(pipelineName) {
        this.shownPipelineNames.push(pipelineName);
    },
    showPipelines: function(pipelineNames) {
        for(var i = 0; i < pipelineNames.length; i++) {
            this.showPipeline(pipelineNames[i]);
        }
    },
    hidePipeline: function(pipelineName) {
        this.shownPipelineNames = this.shownPipelineNames.without(pipelineName);
    },
    hidePipelines: function(pipelineNames) {
        var shouldShow = $A();
        for(var i = 0; i < this.shownPipelineNames.length; i++) {
            if(!pipelineNames.include(this.shownPipelineNames[i])){
                shouldShow.push(this.shownPipelineNames[i]);
            }
        }
        this.shownPipelineNames = shouldShow;
    },
    hasNoHiddenPipeline: function(totalPipelinesCount) {
        return this.shownPipelineNames.length == totalPipelinesCount;
    },
    hasNoShownPipeline: function() {
        return this.shownPipelineNames.length == 0;
    },
    pipelinesCount: function() {
        return this.shownPipelineNames.length;
    },
    inverse: function(allPipelineNames) {
        this._cleanCookie();
        return new Blacklist(arrayMinus(allPipelineNames, this.shownPipelineNames));
    },
    toString: function() {
        return 'whitelist';
    }
});

var PipelineSelectorCookie = Class.create({
    initialize: function(){
        if(getCookie('pipeline-selector-strategy') == 'whitelist'){
            this.strategy = new Whitelist();
        } else {
            this.strategy = new Blacklist();
        }
    },
    persist: function(allPipelineNames){
        if(allPipelineNames) {
            var alwasyUseBlack = allPipelineNames.length < 70;
            if(alwasyUseBlack){
                if(this.strategy.toString() == 'whitelist'){
                    this.strategy = this.strategy.inverse(allPipelineNames);
                }
            } else if (this.strategy.pipelinesCount() > allPipelineNames.length/2){
                this.strategy = this.strategy.inverse(allPipelineNames);
            }
        }
        this.strategy.persist();
    },
    isPipelineHidden: function(pipelineName) {
        return this.strategy.isPipelineHidden(pipelineName);
    },
    showPipeline: function(pipelineName) {
        this.strategy.showPipeline(pipelineName);
    },
    showPipelines: function(pipelineNames) {
        this.strategy.showPipelines(pipelineNames);
    },
    hidePipeline: function(pipelineName) {
        this.strategy.hidePipeline(pipelineName);
    },
    hidePipelines: function(pipelineNames) {
        this.strategy.hidePipelines(pipelineNames);
    },
    hasNoHiddenPipeline: function(totalPipelinesCount) {
        return this.strategy.hasNoHiddenPipeline(totalPipelinesCount);
    },
    hasNoShownPipeline: function(totalPipelinesCount) {
        return this.strategy.hasNoShownPipeline(totalPipelinesCount);
    }
});