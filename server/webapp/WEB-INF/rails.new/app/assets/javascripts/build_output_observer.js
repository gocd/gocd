/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var BuildOutputObserver = Class.create();

BuildOutputObserver.prototype = {
    initialize: function(buildLocator, name) {
        var self = this;
        this.name = name;
        this.buildLocator = buildLocator;
        this.start_line_number = 0;
        this.was_building = false;
        this.is_output_empty = false;
        this.is_completed = false;
        this.ansi_up = ansi_up.ansi_to_html_obj();
        this.enableTailing = true;
        this.window = jQuery(window);
        this.originalWindowScrollTop = this.window.scrollTop();
        this.consoleElement = jQuery('.buildoutput_pre');
        this.consoleTabElement = jQuery('#build_console');
        this.autoScrollButton = jQuery('.auto-scroll');
        this.autoScrollButton.toggleClass('tailing', this.enableTailing);
        this.autoScrollButton.on('click', function(){
            self.enableTailing = !self.enableTailing;
            self.initializeScroll();
        });
        this.consoleTabElement.on('click', function(){
            self.initializeScroll();
        });
        this.initializeScroll();
    },
    initializeScroll: function(){
      if(this.enableTailing){
        this.startScroll();
      } else {
        this.stopScroll();
      }
    },
    startScroll: function(){
        this.autoScrollButton.toggleClass('tailing', this.enableTailing);
        var self = this;
        this.scrollToBottom(0);
        this.window.on('scroll.autoScroll resize.autoScroll', jQuery.throttle(200, function () {
            var windowScrollTop = self.window.scrollTop();
            if (self.originalWindowScrollTop - windowScrollTop > 5) {
                self.stopScroll();
            }
        }));
    },
    stopScroll: function(){
        this.window.off('scroll.autoScroll resize.autoScroll');
        this.enableTailing = false;
        this.autoScrollButton.toggleClass('tailing', this.enableTailing);
    },
    scrollToBottom: function(delay){
        var self = this;
        var captureScrollTop = function () {
          self.originalWindowScrollTop = self.window.scrollTop();
        };

        jQuery('body,html').stop(true).animate(
            {
                scrollTop: this.consoleElement.outerHeight()
            },
            {
                duration: delay || 100,
                start: captureScrollTop, // start is not support with the current version, but we'll be upgrading
                complete: captureScrollTop,
                step: captureScrollTop
            }
        );
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
                        _this.is_output_empty = _this._update_live_output_color(transport.responseText);
                    } else {
                        _this.is_output_empty = true;
                    }
                },
              onFailure: function(response){
                if (404 === response.status){
                  _this.is_output_empty = _this._update_live_output_color(response.responseText);
                } else {
                  var message = "There was an error contacting the server. The HTTP status was " + response.status + ".";
                  _this.is_output_empty = _this._update_live_output_color(message);
                }
              }
            });
        }
    },

    _update_live_output_color: function(build_output) {
        var is_output_empty = !build_output;
        if (!is_output_empty) {

            // parsing the entier console output and building HTML is computationally expensive and blows up memory
            // we therefore chunk the console output into 1000 lines each and hand it over to the parser, and also insert it into the DOM.

            var lines = build_output.match(/^.*([\n\r]+|$)/gm);
            while(lines.length){
                var slice = lines.splice(0, 1000);
                var htmlContents = this.ansi_up.ansi_to_html(slice.join("").escapeHTML(), {use_classes: true});
                this.consoleElement.append(htmlContents);
            }
        }

        if (this.enableTailing){
            this.scrollToBottom();
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
        $('agent_name').update(json.building_info.agent.escapeHTML() + ' (ip:' + json.building_info.agent_ip + ')');
        // TODO: update css on building panel
        json_to_css.update_build_detail_header(json);
    },
    update_date : function(element, content) {
        if (!element) return;
        element.update(content);
    }
}
