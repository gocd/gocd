(function($, _, c, global) {
  "use strict";

  function flash() {
    return document.getElementById("message_pane");
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

    this.list = function list() { return [].slice.call(plugins); };

    this.init = function init() {
      new XhrPromise({
        url: Routes.apiv4AdminPluginInfoIndexPath(),
        type: "GET",
        responseType: "json",
        headers: {
          Accept: "application/vnd.go.cd.v4+json"
        }
      }).then(function (res) {
        plugins = _.filter(res.data._embedded.plugin_info, function(el) {
          return "active" === el.status.state && !!_.find(el.extensions, function(ex) {
            return "configrepo" === ex.type
          })
        });
      }).catch(function (res) {
        Flash.error(res.error.message);
      });
    };
  }

  var pluginInfos = new PluginInfos(); // a private singleton

  function createPluginListOption(p) {
    return c("li", c("a", {class: "plugin-choice", "data-plugin-id": p.id}, p.about.name));
  }

  global.ExportAdapter = {
    init: function init() {
      pluginInfos.init();
    },

    downloadAsFile: function downloadAsFile(blobUrl, name) {
      var a = c("a", {href: blobUrl, download: name, style: "display: none"});

      document.body.appendChild(a); // Firefox requires this to be added to the DOM before click()
      a.click();
      document.body.removeChild(a);
    },

    showExportOptions: function showExportOptions(panel, el) {
      var dropdown = c("ul", {class: "export-plugins-dropdown"}, _.map(pluginInfos.list(), createPluginListOption));
      clear(panel).appendChild(dropdown);

      $(dropdown).on("click", ".plugin-choice", function downloadExport(ev) {
        ev.preventDefault();
        ev.stopPropagation();

        new XhrPromise({
          url: el.getAttribute("href") + "&pluginId=" + encodeURIComponent($(ev.currentTarget).data("plugin-id")),
          responseType: "blob",
          headers: {
            Accept: "application/vnd.go.cd.v7+json"
          },
          beforeSend: function() {
            Flash.clear();
          }
        }).then(function (res) {
          startClientDownload(res.data, res.xhr);
        }).catch(function (res) {
          handleError(res.xhr.status, new FileReader().readAsText(res.error))
        }).finally(function() {
          // hide loading indicator
        });

        function startClientDownload(responseBlob, xhr) {
          var name = xhr.getResponseHeader("Content-Disposition").replace(/^attachment; filename=/, "").replace(/^(")(.+)(\1)/, "$2");
          ExportAdapter.downloadAsFile(URL.createObjectURL(responseBlob), name);
        }

        function handleError(status, responseText) {
          Flash.error(JSON.parse(responseText).message);
        }
      })
    }
  };

})(jQuery, _, crel, window);
