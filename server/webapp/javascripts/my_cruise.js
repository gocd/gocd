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

var MyCruise = Class.create({
    initialize : function(pipelines) {
        this.pipelines = pipelines;
        if (pipelines.length == 0) {
            $('add-notification-filter').disabled = true;
            $('reset-notification-filter').disabled = true;
        }
        pipelines.each(function(pipeline, index) {
            var option = new Option(pipeline.name, pipeline.name);
            this._addSelected(option, index);
            $("pipeline").options[$("pipeline").options.length] = option;
        }.bind(this));

        var edit_actions = $$('#email-settings .edit-actions');
        var save_actions = $$('#email-settings .save-actions');

        this.visible_elements_when_edit = $A([$("email"), $("matchers-block"), save_actions]).flatten();
        this.visible_elements_when_view = $A([$("email-text"), edit_actions]).flatten();

        this.updateStages();
    },
    editMatchers : function() {
        $("emailme").enable();

        this.visible_elements_when_edit.invoke('show');
        this.visible_elements_when_view.invoke('hide');

        $$('#email-settings .matcher').invoke('hide');
    },
    cancelMatchers : function() {
        $("emailme").disable();

        this.visible_elements_when_edit.invoke('hide');
        this.visible_elements_when_view.invoke('show');

        $$('#email-settings .matcher').invoke('show');
    },
    updateStages : function() {
        var pipelines = this.pipelines;
        var pipelineName = $('pipeline').getValue();
        $("stage").options.length = 0;

        for (var i = 0; i < pipelines.length; i++) {
            if (pipelines[i].name == pipelineName) {
                pipelines[i].stages.each(function(stage, index) {
                    var option = new Option(stage.stageName, stage.stageName);
                    this._addSelected(option, index);
                    $("stage").options[$("stage").options.length] = option;
                }.bind(this));
            }
        }
    },
    _addSelected: function (option, index) {
        if (index == 0) {
            option.setAttribute('selected', 'selected');
        }
    },
    deleteFilter :function(filterId) {
        $('notification_form').action = 'notification/delete';
        $('filterId').value = filterId;
        $('notification_form').submit();
    },
    resetFilter :function() {
        $('pipeline').selectedIndex = 0;
        $('stage').selectedIndex = 0;
        $('event').selectedIndex = 0;
    },
    validate : function() {
        this.isSuccess = false;
        var email = $('email').getValue();
        var matchers = $('matchers').getValue();
        
        var caller = this;
        new Ajax.Request(context_path("tab/mycruise/user/validate"), {
            method: 'GET',
            asynchronous: false,
            parameters: {
                email: email,
                matchers: matchers
            },
            onSuccess: function() {
                caller.isSuccess = true;
                $('error-message').hide();
            },
            onFailure: function(transport) {
                caller._invalidInput(transport)
            },
            onExeption: function(transport) {
                caller._invalidInput(transport)
            }
        });
        return this.isSuccess;
    },

    _invalidInput : function (transport) {
        this.isSuccess = false;
        $$('#tab-content-of-notifications .message.info').invoke('hide');
        if($('serverside-message')){
            $('serverside-message').hide();
        }
        var message = $('error-message');
        message.update(transport.responseText.evalJSON().message)
        message.show();
        this.editMatchers();
    }
});
