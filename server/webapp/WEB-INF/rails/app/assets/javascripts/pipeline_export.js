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
    return c("li", {class: "plugin-choice", "data-plugin-id": p.id}, p.about.name);
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

    showExportOptions: function showExportOptions(e) {
      e.preventDefault();
      e.stopPropagation();

      var el = $(e.currentTarget);

      if (el.data("open")) {
        el.find(".export-plugins-dropdown").remove();
        el.data("open", false);
      } else {
        var dropdown = c("ul", {class: "export-plugins-dropdown"}, _.map(pluginInfos.list(), createPluginListOption));
        el.append(dropdown);

        $(dropdown).on("click", ".plugin-choice", function downloadExport(ev) {
          ev.preventDefault();
          ev.stopPropagation();

          var url = el.attr("href") + "&pluginId=" + encodeURIComponent($(ev.currentTarget).data("plugin-id"));

          // Using native XHR so we can work with responseType and response;
          // jQuery ajax does not support this.
          var xhr = new XMLHttpRequest();

          xhr.onreadystatechange = function() {
            if (4 === xhr.readyState) { // request complete
              if (xhr.status < 400 && xhr.status > 199) {
                startClientDownload(xhr.response, xhr);
              } else {
                handleError(xhr.status, new FileReader().readAsText(xhr.response));
              }
            }
          };

          xhr.open("GET", url);
          xhr.responseType = "blob";
          xhr.setRequestHeader("Accept", "application/vnd.go.cd.v7+json");
          xhr.send();

          function startClientDownload(responseBlob, xhr) {
            var name = xhr.getResponseHeader("Content-Disposition").replace(/^attachment; filename=/, "").replace(/^(")(.+)(\1)/, "$2");
            ExportAdapter.downloadAsFile(URL.createObjectURL(responseBlob), name);
          }

          function handleError(status, responseText) {
            console.error(JSON.parse(responseText).message)
          }

          el.find(".export-plugins-dropdown").remove();
          el.data("open", false);
        })

        el.data("open", true);
      }
    }
  };

})(jQuery, _, crel, window);
