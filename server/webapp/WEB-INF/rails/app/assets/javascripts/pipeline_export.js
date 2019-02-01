/*
 * Copyright 2018 ThoughtWorks, Inc.
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

 (function($, _, c, global) {
  "use strict";

  function flash() {
    return document.getElementById("message_pane") || c("div", {id: "message_pane"});
  }

  function clear(el) {
    while (el.firstChild) { el.removeChild(el.firstChild); }
    return el;
  }

  var Flash = {
    clear: function () {
      clear(flash());
    },
    error: function (message) {
      clear(flash()).appendChild(c("p", {class: "error"}, message));
    },
    success: function (message) {
      clear(flash()).appendChild(c("p", {class: "success"}, message));
    }
  }

  function PluginInfos() {
    var plugins = [];

    this.plugins = function copyPlugins() { return [].slice.call(plugins); };

    this.init = function init() {
      new XhrPromise({
        url: Routes.apiv4AdminPluginInfoIndexPath() + "?type=configrepo",
        type: "GET",
        responseType: "json",
        headers: {
          Accept: "application/vnd.go.cd.v4+json"
        }
      }).then(function (res) {
        debugger;
        plugins = _.filter(res.data._embedded.plugin_info, function(el) {
          return "active" === el.status.state;
        });
      }).catch(function (res) {
        Flash.error(res.error.message);
      });
    };
  }

  function createPluginListOption(p) {
    return c("li", c("a", {class: "plugin-choice", "data-plugin-id": p.id}, p.about.name));
  }

  global.PipelineExport = {
    downloadAsFile: function downloadAsFile(blobUrl, name) {
      var a = c("a", {href: blobUrl, download: name, style: "display: none"});

      document.body.appendChild(a); // Firefox requires this to be added to the DOM before click()
      a.click();
      document.body.removeChild(a);
    },

    showExportOptions: function showExportOptions(panel, el, beforeRequest) {
      var dropdown = c("ul", {class: "export-plugins-dropdown"}, _.map(this.plugins(), createPluginListOption));
      clear(panel).appendChild(dropdown);

      $(dropdown).on("click", ".plugin-choice", function downloadExport(ev) {
        ev.preventDefault();
        ev.stopPropagation();

        el.classList.add("loading");
        new XhrPromise({
          url: el.getAttribute("href") + "?plugin_id=" + encodeURIComponent($(ev.currentTarget).data("plugin-id")),
          responseType: "blob",
          headers: {
            Accept: "application/vnd.go.cd.v1+json"
          },
          beforeSend: function() {
            Flash.clear();

            if ("function" === typeof beforeRequest) {
              beforeRequest();
            }
          }
        }).then(function (res) {
          startClientDownload(res.data, res.xhr);
        }).catch(function (res) {
          var reader = new FileReader();
          reader.onloadend = function () { handleError(res.xhr.status, reader.result); };
          reader.readAsText(res.xhr.response);
        }).finally(function() {
          el.classList.remove("loading");
        });

        function startClientDownload(responseBlob, xhr) {
          var name = xhr.getResponseHeader("Content-Disposition").replace(/^attachment; filename=/, "").replace(/^(")(.+)(\1)/, "$2");
          PipelineExport.downloadAsFile(URL.createObjectURL(responseBlob), name);
        }

        function handleError(status, responseText) {
          Flash.error(JSON.parse(responseText).message);
        }
      })
    }
  };

  PluginInfos.call(global.PipelineExport); // mix in PluginInfos capabilities
})(jQuery, _, crel, window);
