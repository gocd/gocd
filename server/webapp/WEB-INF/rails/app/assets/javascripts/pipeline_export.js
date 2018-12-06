(function($, _, c, global) {
  "use strict";

  function PluginInfos() {
    var plugins = [];

    this.list = function list() { return [].slice.call(plugins); };

    this.init = function init() {
      $.ajax({
        url: Routes.apiv4AdminPluginInfoIndexPath(),
        type: "GET",
        dataType: "json",
        beforeSend: function apiHeaders(xhr) {
          xhr.setRequestHeader("Accept", "application/vnd.go.cd.v4+json");
        }
      }).done(function(data) {
        plugins = _.filter(data._embedded.plugin_info, function(el) {
          return "active" === el.status.state && !!_.find(el.extensions, function(ex) {
            return "configrepo" === ex.type
          })
        });
      }).fail(console.error);
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
      panel.innerHTML = "";
      var dropdown = c("ul", {class: "export-plugins-dropdown"}, _.map(pluginInfos.list(), createPluginListOption));
      panel.appendChild(dropdown);

      $(dropdown).on("click", ".plugin-choice", function downloadExport(ev) {
        ev.preventDefault();
        ev.stopPropagation();

        new XhrPromise({
          url: el.getAttribute("href") + "&pluginId=" + encodeURIComponent($(ev.currentTarget).data("plugin-id")),
          responseType: "blob",
          headers: {
            "Accept": "application/vnd.go.cd.v7+json"
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
          console.error(JSON.parse(responseText).message)
        }
      })
    }
  };

})(jQuery, _, crel, window);
