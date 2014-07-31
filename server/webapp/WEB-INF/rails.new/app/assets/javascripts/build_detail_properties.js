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

var BuildDetailProperties = Class.create();
BuildDetailProperties.prototype = {
    initialize: function(pipelineName, label, stageName, jobConfig, buildLocator) {
        this.pipelineName = pipelineName;
        this.label = label;
        this.stageName = stageName;
        this.jobConfig = jobConfig;
        this.buildLocator = buildLocator;
    },
    showEditPane: function(propertyName) {
        $('prop-' + propertyName + '-shadow').show();
        $('prop-' + propertyName).hide();
        this.backupKeyValue(propertyName);
    },
    hideEditPane: function(propertyName) {
        $('prop-' + propertyName + '-shadow').hide();
        $('prop-' + propertyName).show();
    },
    cancelEditPane: function(propertyName) {
        this.restoreKeyValue(propertyName);
        this.hideEditPane(propertyName);
    },
    backupKeyValue: function(propertyName) {
        $('prop-' + propertyName + '-key')._value = $('prop-' + propertyName + '-key').value;
        $('prop-' + propertyName + '-value')._value = $('prop-' + propertyName + '-value').value;
    },
    restoreKeyValue: function(propertyName) {
        $('prop-' + propertyName + '-key').value = $('prop-' + propertyName + '-key')._value;
        $('prop-' + propertyName + '-value').value = $('prop-' + propertyName + '-value')._value;
    },
    showAddPane: function() {
        $('new-property').show();
        $('add-new-property').hide();
        $('new-property').scrollTo();
    },
    cancelAddPane: function() {
        $('new-property').hide();
        $('property-key').value = '';
        $('property-value').value = '';
        $('add-new-property').show();
    },
    saveProperties: function() {
        var key = $('property-key').value;
        var value = $('property-value').value;

        if (!this.validateKey(key)) {
            alert('The property name you specify is invalid, only "a-z", "A-Z", "0-9", "-", "_", ".", "/" is allowed, and the length should be less than 255.');
            $('property-key').focus();
            return;
        }
        var request_url = this.getUrlPath() + key;
        var context = this;

        $('new-property-submit-indicator').show();

        new Ajax.Request(request_url, {
            method: 'post',
            parameters: {
                'value': value
            },
            onSuccess: function() {
                $('new-property-submit-indicator').hide();
                context.andNewProperty(key, value);
                context.cancelAddPane();
            },
            onException: function() {
                alert('Save failed! Unexpected exception!');
                $('new-property-submit-indicator').hide();
            },
            onFailure: function() {
                alert('Save failed!');
                $('new-property-submit-indicator').hide();
            },
            onCompelete: function() {
                $('new-property-submit-indicator').hide();
            },
            on409: function() {
                alert('Save failed! The property you just added is duplicated.');
                $('property-key').focus();
                $('new-property-submit-indicator').hide();
            }
        });
    },
    validateKey: function(key) {
        var rule = /^[\w|\-|_|\.|\/]{1,255}$/;
        if (key && rule.test(key) && key.toLowerCase() != 'null' && key.strip() != '') {
            return true;
        }

        return false;
    },
    updateProperties: function(propertyName) {
        var key = $('prop-' + propertyName + '-key').value;
        var value = $('prop-' + propertyName + '-value').value;

        var request_url = this.getUrlPath() + key;
        var context = this;

        new Ajax.Request(request_url, {
            parameters: {
                'value': value
            },
            method: 'post',
            onSuccess: function() {
                alert('success!');
                context.andNewProperty(key, value);
                context.cancelAddPane();
            }
        });
    },
    andNewProperty: function(key, value) {
        var tr = Builder.node('tr', {id: 'property-of-' + key});
        var td1 = Builder.node('td');
        td1 = $(td1);
        td1.update(key);
        td1.addClassName('first');
        var td2 = Builder.node('td');
        td2 = $(td2);
        td2.update(value);
        var td3 = Builder.node('td');

        tr.appendChild(td1);
        tr.appendChild(td2);
        tr.appendChild(td3);

        var properties_rows = $$('#build-peroperties-table tr');
        var properties_rows_num = properties_rows.length;
        if ((properties_rows_num % 2) == 0) {
            //already has even rows
            $(tr).addClassName('odd');
        } else {
            $(tr).addClassName('even');
        }

        var tbody = $('build-peroperties-table').down('tbody');

        tbody.appendChild(tr);
        tbody.appendChild($('new-property').remove());
    },
    getUrlPath: function() {
        return context_path('properties/' + this.buildLocator + '/');
    }
};