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

var JsonToCss = Class.create();

JsonToCss.prototype = {
    initialize : function() {},

    update_bar : function(json) {
        var planName = json.building_info.name;
        var bar = $(planName + "_bar");
        if (!bar) return;
        var css_class_name = json.building_info.current_status.toLowerCase();
        this._renew_class_name(bar, [css_class_name]);

    },

    update_profile : function(json) {
        var planName = json.building_info.name + '_profile';
        var css_class_name = json.building_info.current_status.toLowerCase();
        this._renew_class_name(planName, [css_class_name]);
    },

    update_force_build : function(json) {
        var element_id = json.building_info.name + '_forcebuild';
        $(element_id).removeClassName("force_build_disabled");
        $(element_id).removeClassName("force_build_enabled");
        if (should_forcebuild_be_disabled(json)) {
	        $(element_id).addClassName("force_build_disabled");
        } else {
	        $(element_id).addClassName("force_build_enabled");
        }    
    },
    update_tooltip : function(json) {
        var planName = json.building_info.name;
        var tooltip = $('tooltip_' + planName);
        if (!tooltip) return;
        tooltip.className = '';
        var css_class_name = json.building_info.current_status.toLowerCase();
        this._renew_class_name(tooltip, ['tooltip', css_class_name]);
    },

    update_build_detail_header : function(json) {
        var css_class_name = json.building_info.current_status.toLowerCase();
        this._renew_class_name('build_status', [css_class_name]);
        this._renew_class_name('job_details_header', [css_class_name]);
    },

	update_build_list : function(json, id, imgSrc) {
		var elementId = "build_list_" + id
		var css_class_name = json.building_info.current_status.toLowerCase();
        this._renew_class_name(elementId, [css_class_name]);
        if( css_class_name == "cancelled"){
            var colorCodeElement = $(elementId).getElementsByClassName("color_code_small")[0];
            var img = document.createElement('img');
            img.setAttribute('src', imgSrc);
            colorCodeElement.appendChild(img);
        }
	},
    
    _renew_class_name : function(elementOrId, cssClasses) {
        var element = $(elementOrId);
        clean_active_css_class_on_element(element);
        $A(cssClasses).each(function(cssClass) {
            Element.addClassName(element, cssClass);
        });
    }
}

var json_to_css = new JsonToCss();

var Status = Class.create();

Status.prototype = {
    initialize : function (status) {
        this.status = status.toLowerCase();
    },

    text : function() {
        return this.status;
    },

    is_queued : function() {
        return this.status == "scheduled" || this.status == "assigned";
    },

    is_building : function() {
        return this.status == "preparing" || this.status == "building" || this.status == "completing";
    },
    is_paused : function() {
        return this.status == "paused" ;
    },
    is_discontinued : function() {
        return this.status == "discontinued";
    }
};