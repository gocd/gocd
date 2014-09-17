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

/* Agent Observer: check the agents status in specific interval. */
var AgentObserver = Class.create();

AgentObserver.prototype = {
    initialize : function() {},
    notify : function(jsonArray) {
        $('agent-list').innerHTML = this.getTemplate().process({objs: jsonArray})
    },
    getTemplate : function() {
        if(!this._template){
            this._template = TrimPath.parseDOMTemplate('agent-template');
        }
        return this._template;
    }
};

var InputChecker = Class.create();
InputChecker.prototype = {
    initialize : function(elementId, errorPanelId){
        this.element = $(elementId);
        this.errorPanel = $(errorPanelId);
        Event.observe(this.element, 'keypress', this.onEvent.bindAsEventListener(this));
        Event.observe(this.element, 'keydown', this.onEvent.bindAsEventListener(this))
    },
    rule : /^[a-zA-Z0-9_\-\|\.\s,]*$/,
    match : function() {
        return this.rule.test(this.element.value);
    },
    check : function () {
        if(!this.match()){
            this.errorPanel.show();
        } else {
            this.errorPanel.hide();
        }
        return false;
    },
    onEvent : function(event){
        if(this.submitFunction && event.keyCode==Event.KEY_RETURN){
            this.submitFunction(event);
            return;
        }
        setTimeout( this.check.bind(this), 200);
    }
};
/* Functions of agent tab */
var AgentPage = Class.create({
    initialize: function(){
        this.add_resources_input_checker = new InputChecker('resources', 'edit_resources_panel_error');
        this.add_resources_input_checker.submitFunction = this.addResources.bind(this);
        this.collapsedGroups = $A(['Denied']);//The denied group is hidden by default
    },
    registerAgent: function(agentId, ipAddress){
        $(agentId + '_transport_spinner').show();
        var page = this;
        new Ajax.Request(context_path("registerAgent.json"), {
            method: 'POST',
            parameters: {
                'uuid' : agentId
            },
            onSuccess: page.callBack,
            on406: function(transport) {
                var json = transport.responseText.evalJSON();
                if (json.error) {
                    FlashMessageLauncher.error('Error while approving agent', json.error);
                }
            }

        });
    },
    denyAgent: function(agentUUID){
        $(agentUUID + '_transport_spinner').show();
        var registerAgentUrl = context_path("denyAgent.json");
        var page = this;
        new Ajax.Request(registerAgentUrl, {
            method: 'POST',
            parameters: {
                'uuid' : agentUUID
            },
            onSuccess: page.callBack,
            on406: function(transport) {
                var json = transport.responseText.evalJSON();
                if (json.error) {
                    FlashMessageLauncher.error('Error while denying agent', json.error);
                }
            }
        });
    },
    callBack: function(transport){
        var json = transport.evalJSON();
        if(json.result == 'success'){
            dashboard_periodical_executer.fireNow();
        } else {
            alert(json.error);
        }
    },
    showEditResourcePanel: function(agentId) {
        Agents.getAgent(agentId).show_edit_resources_panel();
    },
    addResources: function() {
        if(this.add_resources_input_checker.match()){
            var agentId = $('agentId').value;
            Agents.getAgent(agentId).edit_resources();
        }
    },
    shouldThisAgentGroupBeCollapsed: function(groupName){
        return this.collapsedGroups.include(groupName);
    },
    toggleAgentGroupStatus: function(groupName, link){
        var agents_container_id = 'agents-of-' + groupName.replace(/\s/ig, '_');
        if(this.collapsedGroups.include(groupName)){
            $(link).removeClassName('collapse-closed').addClassName('collapse-open');
            this.collapsedGroups = this.collapsedGroups.without(groupName);
            $(agents_container_id) && $(agents_container_id).show(); 
        } else {
            $(link).removeClassName('collapse-open').addClassName('collapse-closed');
            this.collapsedGroups.push(groupName);
            $(agents_container_id) && $(agents_container_id).hide();
        }
    }
});