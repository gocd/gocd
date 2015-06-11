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
