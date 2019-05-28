/*
 * Copyright 2019 ThoughtWorks, Inc.
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
(function($) {
  "use strict";

  window.Analytics = {
    modal: function(options) {
      var div = document.createElement("div");

      PluginEndpointRequestHandler.defineLinkHandler();
      PluginEndpoint.ensure("v1");
      $(div).addClass("analytics-plugin").dialog({
        title: options.title || "Analytics",
        width: 850,
        height: 525,
        modal: true,
        close: function(e, ui) {
          $(div).remove();
        }
      });

      $.ajax({
        url: options.url,
        dataType: "json",
        data: options.data,
        type: options.type || "GET"
      }).done(function(r) {
        var frame = document.createElement("iframe");
        frame.sandbox = "allow-scripts";

        frame.onload = function(e) {
          PluginEndpoint.init(frame.contentWindow, {initialData: r.data})
        };

        div.appendChild(frame);
        frame.setAttribute("src", r.view_path);
      }).fail(function(xhr) {
        if (xhr.getResponseHeader("content-type").indexOf("text/html") !== -1) {
          var frame = document.createElement("iframe");
          frame.src = "data:text/html;charset=utf-8," + xhr.responseText;
          div.appendChild(frame);
          return;
        }
        var errorEl = document.createElement("div");
        $(errorEl).addClass("error");
        errorEl.textContent = xhr.responseText;
        div.appendChild(errorEl);
      });
    }
  };

})(jQuery);
