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

function isCollapsed(heading_element) {
  return heading_element.hasClassName('collapsed-heading');
}

function toggleCollapse(heading_element, placeholder) {
	if (isCollapsed(heading_element)) {
		heading_element.removeClassName('collapsed-heading');
		heading_element.addClassName('collapsible-heading');
		heading_element.nextSiblings()[0].removeClassName('collapsed');
	} else {
		heading_element.removeClassName('collapsible-heading');
		heading_element.addClassName('collapsed-heading');
		heading_element.nextSiblings()[0].addClassName('collapsed');
	}
}

function openFromUrl() {
  var url = window.location.href;
  if (url.lastIndexOf('#') > -1) {
    var sectionName = url.substring(url.lastIndexOf('#') + 1, url.length);
    var heading_element = $(sectionName);
    if (isCollapsed(heading_element)) {
      toggleCollapse(heading_element);
    }
  }
}

Event.observe(window, 'load',
  function () { openFromUrl(); }
);
