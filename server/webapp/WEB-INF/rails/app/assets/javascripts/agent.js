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

var Agent = Class.create();
Agent.prototype = {
    initialize : function(uuid) {
        this.uuid = uuid;
    },
    get_resources : function() {
        var span_array = $(this.uuid).select('.resources li span');
        var resources = [];
        span_array.each(function(span) {
            resources.push(span.innerHTML);
        })
        return resources;
    },
    show_edit_resources_panel : function() {
        $("agentId").value = this.uuid;
        $("resources").value = "";
        $('edit_resources_panel_error').hide();
        var reference_element = $(this.uuid).down('ul', 0);
        Position.clone(reference_element, $('edit_resources_panel'), {setHeight: false, setWidth: false, offsetTop: reference_element.offsetHeight});
        $("edit_resources_panel").show();
        $("resources").focus();
    },
    edit_resources: function() {
        var params = {};
        params["agentId"] = this.uuid;
        var resources = this.get_resources();
        var existing = resources ? resources.join(',') : '';
        params["agentResources"] = existing + "," + $("resources").value;
        this._remote_edit_resources(params);
    },
    remove_resource : function(resource) {
        var params = new Object;
        params["agentId"] = this.uuid;
        params["agentResources"] = this.get_resources().without(resource).join(',');
        this._remote_edit_resources(params);
    },
    edit_resources_on_success : function(json) {
        this.finished_saving_resource();
        var template = $('agent-template').value;
        $('agent-list').innerHTML = template.process({objs: json});
    },
    error_saving_resource : function(errorMessage) {
        this.finished_saving_resource();
        FlashMessageLauncher.error(errorMessage);
    },
    finished_saving_resource: function() {
        this._hide_spinner();
        $("edit_resources_panel").hide();
        FlashMessageLauncher.hide('error');
    },
    _remote_edit_resources : function(params) {
        var agent = this;
        this._show_spinner();
        new Ajax.Request(context_path("addresources.json"), {
            asynchronous:1,
            method: 'POST',
            parameters : params,
            on409: function(transport) {
                var json = transport.responseText.evalJSON();
                agent.error_saving_resource(json.error_message);
            },
            onFailure: function(transport) {
                agent.error_saving_resource("Error updating resources.")
            },
            onSuccess: function(transport) {
                var json = transport.responseText.evalJSON();
                agent.edit_resources_on_success(json);
            }
        });
    },
    _show_spinner : function() {
        $('edit_resources_panel_transport_spinner').show();
    },
    _hide_spinner : function() {
        $('edit_resources_panel_transport_spinner').hide();
    }
};

var Agents = Class.create();
Agents.cache = $A([]);
Agents.getAgent = function(uuid) {
    if (!Agents.cache[uuid]) {
        Agents.cache[uuid] = new Agent(uuid);
    }
    return Agents.cache[uuid];
};