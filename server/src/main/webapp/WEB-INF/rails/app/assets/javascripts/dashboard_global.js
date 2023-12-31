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
function context_path(path_info) {
  if (path_info && path_info.startsWith(contextPath)) {
    return path_info;
  }
  var pathSeparator = (contextPath.endsWith("/") || path_info.startsWith("/") ? "" : "/");
  return contextPath + pathSeparator + path_info;
}

var ACTIVE_STATUS = ['passed', 'failed', 'inactive', 'discontinued', 'paused', 'queued', 'scheduled', 'assigned',
  'preparing', 'building', 'completing', 'building_passed',
  'building_failed', 'building_unknown', 'unknown',
  'level_0', 'level_1', 'level_2', 'level_3', 'level_4', 'level_5', 'level_6', 'level_7', 'level_8'];

function clean_active_css_class_on_element(element) {
  ACTIVE_STATUS.forEach(function (status) {
    $(element).removeClass(status);
    $(element).removeClass('build_profile_' + status);
  });
}

function is_result_unknown(json) {
  return json.building_info.result.toLowerCase() == 'unknown';
}

// noinspection JSUnusedGlobalSymbols May be used from FreeMarker template
function isEstimatable(status) {
  if (!status) return false;
  var buildStatus = status.toLowerCase();
  return buildStatus == 'building' || buildStatus == 'completing';
}
