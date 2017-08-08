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

var TabsContainer = Class.create({
    initialize: function(element, type, id, subtab_callback_map){
        var container_parent = $(element);
        var container = container_parent.down('ul');
        this.container = container;
        this.container.tab_type = type;
        this.container.tab_id = id;
        container.tabs = container.select('li').filter(function(tab) { return ! tab.hasClassName('external_pointing'); }).collect(function(tab){
                var tab_name = jQuery(tab).find('.tab_button_body_match_text').text();
                return (subtab_callback_map && subtab_callback_map[tab_name]) || new SubTabs(tab);
        });
        container.hideAllSubTabs = this.hideAllSubTabs.bind(this);
    },
    hideAllSubTabs: function(){
        this.container.tabs.each(function(tab){
            tab.hideContent();
        });
    }
});
var SubTabs = Class.create({
    initialize: function(element, before_open_callback){
        var element = $(element);
        this.element = element;
        this.container = element.parentNode;
        this.link = element.down('a', 0);
        this.initializeLinkAndContent();
        element.open = this.open.bind(this);
        element.observe('click', this.handleTabClick.bindAsEventListener(this));
        this.before_open_callback = before_open_callback;
    },
    initializeLinkAndContent: function(){
        var content_name = this.element.down('a.tab_button_body_match_text', 0).innerHTML;
        this.tab_name = content_name;
        this.link.id = 'tab-link-of-' + content_name;
        this.link.href = 'javascript:void(0)';
        var content_id = 'tab-content-of-' + content_name;
        this.content = $(content_id);
    },
    hideContent: function(){
        this.element.removeClassName('current_tab');

        if(this.content){
            this.content.hide();
        }
    },
    handleTabClick: function(event) {
        if (event) {
            var target = jQuery(event.target);
            var hasDisabledParent = (target.parents("li.disabled").length > 0);
            var isDisabled = target.hasClass("disabled");
            if (!(hasDisabledParent || isDisabled)) {
                this.open(event);
            }
        }
    },
    open: function(event){
        if(event != true){
            try{
                window.location.hash = '#';
            }catch(e){}
        }
        this.container.hideAllSubTabs();
        this.element.addClassName('current_tab');
        if (this.before_open_callback) {
            this.before_open_callback(this.tab_name);
        }
        if (this.content) {
            this.content.show();
            var init_method_name = this.content.id+ "_callback";
            if(window[init_method_name]){
                window[init_method_name]();
            }
        }
        TabsManager.prototype.updateLinkToThisPage(this.tab_name);
    }
});
var TabsManager = Class.create({
    initialize: function(tab, type, id, defaultTabName, subtab_callback_map){
        this.type = type;
        this.unique_id = id;
        this.containers = [];
        this.defaultTabName = defaultTabName;
        this.bindTabsObserver(subtab_callback_map);
        this.initializeCurrentTab(tab);
    },
    bindTabsObserver: function(subtab_callback_map){
        var type = this.type;
        var id = this.unique_id;
        var self = this;
        $$('.sub_tabs_container').each(function(tabs_container){
            self.containers.push(new TabsContainer(tabs_container, type, id, subtab_callback_map));
        });
    },
    subTabByName: function(name) {
        for(var i = 0; i < this.containers.length; i++) {
            var container = this.containers[i].container;
            for(var j = 0; j < container.tabs.length; j++) {
                var tab = container.tabs[j];
                if (tab.tab_name === name) {
                    return tab;
                }
            }
        }
    },
    getCurrentTab: function(tab) {
        var tabName = tab;

        if(tabName){
            return tabName;
        } else {
            tabName = this.getCurrentTabFromUrl();
            if(tabName){
                return tabName;
            }else {
              if (this.defaultTabName) {
                return this.defaultTabName;
              }
              return null;
            }
        }
    },
    getCurrentTabFromUrl: function() {
        var url = window.location.href;

        try {
            if(url.lastIndexOf('#tab-') > -1) {
                var tabName = url.substring(url.lastIndexOf('#tab-') + 5, url.length);
                return tabName;
            }
        } catch(e) {}

        return null; // return undefined if no name in the tail of URL
    },
    initializeCurrentTab: function(tab) {
        var current_tab_content_id = this.getCurrentTab(tab);
        if (current_tab_content_id) {
            var current_tab_link = $('tab-link-of-' + current_tab_content_id);
            if (current_tab_link) {
                current_tab_link.parentNode.open(true);
            }
            this.updateLinkToThisPage(current_tab_content_id);
        }
    },
    updateLinkToThisPage: function(tabName){
        var url = window.location.href;
        var url_without_hash = url.lastIndexOf('#') > -1 ? url.substring(0, url.lastIndexOf('#')) : url;
        $('link-to-this-page') && $('link-to-this-page').href && ($('link-to-this-page').href = url_without_hash + '#tab-' + tabName);
    }
});
