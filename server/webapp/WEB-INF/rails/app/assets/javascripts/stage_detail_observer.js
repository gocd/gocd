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

var StageObserver = Class.create();
StageObserver.prototype=
{
    initialize: function(){
        this.building_status = $A([]);
    },
    notify: function(json){
        this.renderHeader(json);
        this.reload_page_if_necessary(json);
    },
    renderHeader: function(json){
        $('stage-detail-header').innerHTML = this.getHeaderTemplate().process({object: json});
    },
    getHeaderTemplate: function(){
        if(!this._headerTemplate){
            this._headerTemplate = TrimPath.parseDOMTemplate('stage-detail-header-template');
        }
        return this._headerTemplate;
    },
    reload_page_if_necessary : function(json) {
        var builds = json.stage.builds;
        for(i=0; i<builds.length; i++){
            var is_completed = builds[i].is_completed.toLowerCase()=="true";
            this.reload_page(i,is_completed);
        }
    },
    reload_page : function(i, build_is_completed) {
        if (!this.building_status[i] && !build_is_completed) {
            this.building_status[i] = true;
        } else if (this.building_status[i] && build_is_completed) {
            window.location.reload();
        }
    }
};

var StageDetailPage = Class.create({
    initialize: function(){
        this.hide_all_builds = false;
    },
    toggleAllBuilds: function(){
        var button = $('collapse-or-expand-builds-button');
        if(button.hasClassName('expended')){
            button.addClassName('collapsed').removeClassName('expended');
            $('stage-buildplans-histories').hide();
            this.hide_all_builds = true;
        } else {
            button.addClassName('expended').removeClassName('collapsed');
            $('stage-buildplans-histories').show();
            this.hide_all_builds = false;
        }
    },
    shouldHideBuilds: function(){
        return this.hide_all_builds;
    }
});