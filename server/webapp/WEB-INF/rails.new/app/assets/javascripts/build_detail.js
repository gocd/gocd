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

var BuildDetail = {
    getSubContainer: function(element) {
        function lookup_element_with_class(elements, class_name) {
            var element;
            for (var i = 0; i < elements.length; i++) {
                if (elements[i].hasClassName(class_name)) {
                    element = elements[i];
                    break;
                }
            }
            return element;
        }
        var dirContainer = lookup_element_with_class($(element).ancestors(), "dir-container");
        return lookup_element_with_class($(dirContainer).nextSiblings(), "subdir-container");
    },
    expandAll: function() {
        $$('.files .directory a').each(function(element) {
            BuildDetail.tree_navigator(element)
        })
    },
    collapseAll: function() {
        $$('.files .opened_directory a').each(function(element) {
            BuildDetail.tree_navigator(element)
        })
    },
    tree_navigator: function (element) {
        var subDirElement = BuildDetail.getSubContainer(element);
        var spanElem = $(element).ancestors()[0];
        if (subDirElement.visible()) {
            spanElem.removeClassName("opened_directory");
            spanElem.addClassName("directory");
            subDirElement.hide();
        } else {
            spanElem.removeClassName("directory");
            spanElem.addClassName("opened_directory");
            subDirElement.show();
        }
    },
    toggleDetailContent: function(event){
        var link = event.target;
        var content = link.nextSiblings().first();
        if(link.match('h2.collapsible_title')){
            if(link.hasClassName('title_message_expanded')){
                link.removeClassName('title_message_expanded').addClassName('title_message_collapsed');
                if(content && content.hasClassName('collapsible_content')){
                    content.hide();
                }
            } else {
                link.removeClassName('title_message_collapsed').addClassName('title_message_expanded');
                if(content && content.hasClassName('collapsible_content')){
                    content.show();
                }
            }
        }
    }
};

var BuildOutputDetector = Class.create({
    initialize: function(pipelineId, stageName, debugMode){
        this.url = context_path('files/pipeline/' + pipelineId + '/stage/' + stageName + '/cruise-output/console.log');
        if(debugMode != undefined){
            this.checkIfOutputAvailable();
        } else {
            this.checkIfOutputAvailable.bind(this).delay(0.1);
        }
    },
    checkIfOutputAvailable: function(){
        new Ajax.Request(this.url, {
            method: 'HEAD',
            on404: this.showNoOutputWarnning,
            onFailure: this.showNoOutputWarnning,
            onExeption: this.showNoOutputWarnning,
            onSuccess: this.updateIframeSrc.bind(this)
        });
    },
    showNoOutputWarnning: function(){
        try{
            $('build-output-console-warnning').show();
        }catch(e){}
    },
    updateIframeSrc: function(){
        var iframe = $('build-output-console-iframe');
        if(iframe){
            iframe.src = this.url;
        }
    }
});
