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

var BuildOutputObserver = Class.create();

BuildOutputObserver.prototype = {
    initialize: function(buildLocator, name) {
        this.name = name;
        this.buildLocator = buildLocator;
        this.start_line_number = 0;
        this.was_building = false;
        this.is_output_empty = false;
        this.is_completed = false;
    },
    notify : function(jsonArray) {
        for (var i = 0; i < jsonArray.length; i++) {
            if (!jsonArray[i]) return;
            var nameMatches = jsonArray[i].building_info.name == this.name;
            if (nameMatches) {
                this._notify(jsonArray[i]);
            }
        }
    },
    _notify : function(json) {
        this.update_build_detail_summary_status(json);
        this.is_completed = json.building_info.is_completed.toLowerCase() == "true";

        if (this.is_completed && this.is_output_empty) {
            this.update_page(json);
            dashboard_periodical_executer.pause();
        }
        else {
            this.update_live_output.bind(this).delay(5);
        }
        this.reload_page(this.is_completed);
    },
    update_live_output : function() {
        var _this = this;
        if (!this.is_completed) {
            var ajaxRequest = new Ajax.Request(context_path("files/" + _this.buildLocator), {
                asynchronous:false,
                method: 'GET',
                parameters: 'startLineNumber=' + _this.start_line_number,
                onSuccess: function(transport, next_start_as_json) {
                    if (next_start_as_json) {
                        _this.start_line_number = next_start_as_json[0];
                        var build_output = transport.responseText;
                        _this.is_output_empty = _this._update_live_output(build_output);
                    } else {
                        _this.is_output_empty = true;
                    }
                }
            });
        }
    },
    _update_live_output: function (build_output) {
        var is_output_empty = !build_output;
        if (!is_output_empty) {
            var escapedOutPut = build_output.escapeHTML();
            if (Prototype.Browser.IE) {
                // Fix for the IE not wrap /r in pre bug
                escapedOutPut = '<br/>' + escapedOutPut.replace(/\n/ig, '<br\/>');
            }
            if($('buildoutput_pre')){
                $('buildoutput_pre').innerHTML += escapedOutPut;
            }
        }
        return is_output_empty;
    },
    update_page : function(json) {
        this.update_build_detail_summary_result(json);
        this.display_error_message_if_necessary(json);
    },
    reload_page : function(build_is_completed) {
        if (!this.was_building && !build_is_completed) {
            this.was_building = true;
        } else if (this.was_building && build_is_completed) {
            window.location.reload();
        }
    },
    display_error_message_if_necessary : function(json) {
        if (is_result_unknown(json)) {
            var text = $$WordBreaker.break_text(json.building_info.name); //TODO: add log folder
            $('trans_content').update("Failed to find log in <br/>" + text);
            new TransMessage('trans_message', $('build_detail_summary_container'), {type:TransMessage.TYPE_ERROR, autoHide:false, height:50});
        }
    },
    update_build_detail_summary_result : function (json) {
        var result = json.building_info.result.toLowerCase();
        $('build_name_status').update(result);
        var div = $$('.build_detail_summary')[0].ancestors()[0];
        clean_active_css_class_on_element(div);
        $(div).addClassName(result.toLowerCase());
    },
    update_build_detail_summary_status : function(json) {
        var status = json.building_info.current_status.toLowerCase();
        $('build_name_status').update(status);
        this.update_date($('build_scheduled_date'), json.building_info.build_scheduled_date);

        this.update_date($('build_assigned_date'), json.building_info.build_assigned_date)
        this.update_date($('build_preparing_date'), json.building_info.build_preparing_date)
        this.update_date($('build_building_date'), json.building_info.build_building_date)
        this.update_date($('build_completing_date'), json.building_info.build_completing_date)
        this.update_date($('build_completed_date'), json.building_info.build_completed_date)
        $('agent_name').setAttribute("href", context_path("agents/" + json.building_info.agent_uuid));
        $('agent_name').update(json.building_info.agent + ' (ip:' + json.building_info.agent_ip + ')');
        // TODO: update css on building panel
        json_to_css.update_build_detail_header(json);
    },
    update_date : function(element, content) {
        if (!element) return;
        element.update(content);
    }
}
