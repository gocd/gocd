/*
 * Copyright 2023 Thoughtworks, Inc.
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
var BuildDetail = {
  getSubContainer: function(element) {
    function lookup_element_with_class(elements, class_name) {
      var element;
      for (var i = 0; i < elements.length; i++) {
        if (elements[i].hasClassName(class_name)) {
          element = elements[i];
          break;
        }
      }
      return element;
    }
    var dirContainer = lookup_element_with_class($(element).ancestors(), "dir-container");
    return lookup_element_with_class($(dirContainer).nextSiblings(), "subdir-container");
  },
  expandAll: function() {
    $$('.files .directory a').each(function(element) {
      BuildDetail.tree_navigator(element);
    });
  },
  collapseAll: function() {
    $$('.files .opened_directory a').each(function(element) {
      BuildDetail.tree_navigator(element);
    });
  },
  tree_navigator: function (element) {
    var subDirElement = BuildDetail.getSubContainer(element);
    var spanElem = $(element).ancestors()[0];
    if (subDirElement.visible()) {
      spanElem.removeClassName("opened_directory");
      spanElem.addClassName("directory");
      subDirElement.hide();
    } else {
      spanElem.removeClassName("directory");
      spanElem.addClassName("opened_directory");
      subDirElement.show();
    }
  }
};
