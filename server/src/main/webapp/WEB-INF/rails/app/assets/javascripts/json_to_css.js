/*
 * Copyright 2024 Thoughtworks, Inc.
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

class JsonToCss {

  update_build_detail_header(json) {
    const css_class_name = json.building_info.current_status.toLowerCase();
    this._renew_class_name('#build_status', [css_class_name]);
    this._renew_class_name('#job_details_header', [css_class_name]);
  }

  update_build_list(json, id, imgSrc) {
    const elementId = "#build_list_" + id;
    const css_class_name = json.building_info.current_status.toLowerCase();
    this._renew_class_name(elementId, [css_class_name]);
    if (css_class_name === "cancelled") {
      const colorCodeElement = $(elementId).get(0).getElementsByClassName("color_code_small")[0];
      const img = document.createElement('img');
      img.setAttribute('src', imgSrc);
      colorCodeElement.appendChild(img);
    }
  }
    
  _renew_class_name(elementOrSelector, cssClasses) {
    const element = $(elementOrSelector);
    clean_active_css_class_on_element(element[0]);
    cssClasses.forEach(function(cssClass) {
      element.addClass(cssClass);
    });
  }
}

const json_to_css = new JsonToCss();
