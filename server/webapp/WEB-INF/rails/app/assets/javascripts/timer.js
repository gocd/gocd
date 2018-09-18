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

var Timer = Class.create();
Timer.LABEL_REMAINING = "ETA:";
Timer.LABEL_ELAPSED = "Elapsed:";
Timer.LABEL_LONGER_BY = "Longer by:";

Timer.prototype = {
    initialize: function(name) {
        var _timer = this;
        this.context_node = null;
        this.elapsed_time = 0;
        this.name = name;
        this.stopped = true;
        this.executer = new PeriodicalExecuter(function(){_timer.update()}, 1);
        this.executer.stop();
    },
    last_duration:function(duration) {
        this.duration = duration;
    },
    set_elapsed_time:function(already_elapsed_time) {
        this.elapsed_time = already_elapsed_time;
    },
    get_elapsed_time:function(){
        return this.elapsed_time;
    },
    update:function() {
        this.elapse();
        this.update_element_content(this.name + '_time_remaining_label', this.get_timer_label());
        this.update_element_content(this.name + '_time_remaining', this.report_remaining());
        this.update_progress_bar(this.name + '_progress_bar', this.name + '_progress');
    },
    update_element_content:function(id, text) {
        var element = $(id)
        if(element) {
            if (text != '') {
                element.update(text);
            }
        }
    },
    get_timer_label: function() {
        if(this.duration){
            if(this.elapsed_time > this.duration) {
                return Timer.LABEL_LONGER_BY;
            } else {
                return Timer.LABEL_REMAINING;
            }
        } else {
            return Timer.LABEL_ELAPSED;
        }

        return Timer.LABEL_ELAPSED;
    },
    update_progress_bar: function(progress_bar_id, progress_id) {
        try{
            if(this.duration){
                if($(progress_id)){
                    $(progress_id).setStyle({width: this.get_progress_percentage() + '%'});
                }
                $(progress_bar_id).show();
            } else {
                $(progress_bar_id).hide();
            }
        } catch(e) {}
    },
    get_progress_percentage: function(){
        if(this.elapsed_time < this.duration){
            return this.elapsed_time * 100 / this.duration;
        } else {
            return 100;
        }
    },
    start:function() {
        //		this.toggle(function(elem){return !elem.visible()});
        this.executer.registerCallback();
        this.stopped = false;
    },
    stop:function() {
        //        this.toggle(function(elem){return elem.visible()});
        this.stopped = true;
        this.elapsed_time = 0;
        this.executer.stop();
        this.update_element_content(this.name + '_time_elapsed', '')
        this.update_element_content(this.name + '_time_elapsed_label', '')
    },
    toggle : function(need_toggle) {
        var elements;
        if (this.context_node) {
            elements = this.context_node.select(".timer_area");
        } else {
            elements = $$(".timer_area");
        }
        elements.each(function(elem){
            if (need_toggle(elem)) {
                Element.toggle(elem);
            }
        });
    },
    is_stopped:function() {
        return this.stopped;
    },
    getPeriodicalExecuter : function() {
        return this.executer;
    },
    seconds_to_minute : function(sec) {
        return Math.floor(sec/60)
    },
    minutes_to_hr : function(min) {
        return this.seconds_to_minute(min)
    },
    time : function(sec) {
        var min = this.seconds_to_minute(sec);
        var hr = this.minutes_to_hr(sec/60);
        return this.digit_pad(hr) + ":" + this.digit_pad(min % 60) + ":" + this.digit_pad(sec % 60);
    },
    elapse : function() {
        this.elapsed_time++;
    },
    report_elapsed : function() {
        return this.time(this.elapsed_time)
    },
    report_remaining : function() {
        return this.time(Math.abs(this.get_last_duration() - this.elapsed_time));
    },
    digit_pad : function (digit) {return ((digit > 9)?"":"0") + digit},
    get_name : function() {
        return  this.name;
    },
    get_last_duration : function() {
        if (!this.duration) return 0;
        return this.duration;
    }
}
