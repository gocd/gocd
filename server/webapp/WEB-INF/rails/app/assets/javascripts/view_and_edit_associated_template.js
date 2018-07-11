/*
 * Copyright 2017 ThoughtWorks, Inc.
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

var viewAndEditAssociatedTemplate = function (templateSelector) {

  var addLinks = function (templateMap, parameterizedViewLink, parameterizedEditLink) {
    if (areTemplatesDefined()) {
      var links = jQuery("#links");
      links.html('');
      var canView = templateMap[selectedTemplateName()].canView;
      var canEdit = templateMap[selectedTemplateName()].canEdit;
      var editLink, viewLink;

      if (canView) {
        var templateViewUrl = replaceUrlWithSelectedTemplate(parameterizedViewLink);
        viewLink            = jQuery('<a>').attr({
          href:  templateViewUrl,
          class: 'view_template_link action_icon button view_icon skip_dirty_stop',
        }).html('View');

        viewLink.on('click', function () {
          Util.ajax_modal(templateViewUrl, {overlayClose: false, title: selectedTemplateName()}, function (text) {
            return text;
          });
          return false;
        });
      }
      else {
        viewLink = jQuery('<span>').attr({
          class: 'view_template_link action_icon button view_icon_disabled skip_dirty_stop',
          title: "Unauthorized to view template"
        }).html('View');
      }

      if (canEdit) {
        var constructedTemplateEditUrl = replaceUrlWithSelectedTemplate(parameterizedEditLink);
        editLink                       = jQuery('<a>').attr({
          href:  constructedTemplateEditUrl,
          class: 'action_icon edit_icon edit_template_link',
        }).html('Edit');

      }
      else {
        editLink = jQuery('<span>').attr({
          class: 'action_icon edit_icon_disabled edit_template_link',
          title: "Unauthorized to edit template"
        }).html('Edit');
      }

      links.append(viewLink);
      links.append(editLink);
    }
  };

  var selectedTemplateName = function () {
    return jQuery(templateSelector).val();
  };

  var areTemplatesDefined = function () {
    return selectedTemplateName() ? true : false;
  };

  var replaceUrlWithSelectedTemplate = function (parameterizedUrl) {
    return parameterizedUrl.replace("__template_name__", selectedTemplateName());
  };

  return {
    addViewAndEditTemplateLinks: addLinks
  };
};
