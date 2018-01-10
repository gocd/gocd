(function() {
  "use strict";

  const m = require("mithril");
  const $ = require("jquery");

  require("foundation-sites");

  const PluginEndpoint     = require('rails-shared/plugin-endpoint');
  const VersionUpdater     = require('models/shared/version_updater');
  const PluginiFrameWidget = require('views/analytics/plugin_iframe_widget');
  const Routes             = require('gen/js-routes');

  PluginEndpoint.ensure();

  PluginEndpoint.define({
    "analytics.fetch-analytics-for-pipeline": (message, reply) => {
      $("iframe[uid='" + message.uid + "']")[0];

      $.ajax({
        url: Routes.pipelineAnalyticsPath({plugin_id: message.pluginId, pipeline_name: message.data.pipelineName}),
        type: "GET",
        dataType: "json"
      }).done((r) => {
        console.log(r);
        console.log($("iframe[uid='" + message.uid + "']")[0]);
      });
    }
  });

  document.addEventListener("DOMContentLoaded", () => {
    const main = document.getElementById("analytics-container");
    let currentUid = 0;
  //loop plugin ids (data attribute), mount iframe widget, make call to dashboard (return viewpath + data), send message to iframe with data, set src to viewpath
    m.mount(main, {
      view() {
        return $.map($(main).data("plugin-ids"), (id) => {
          ++currentUid;
          return m(PluginiFrameWidget, {url: Routes.dashboardAnalyticsPath({plugin_id: id}), pluginId: id, uid: "f-" + currentUid});
        });
      }
    })

    // boilerplate to init menus and check for updates
    $(document).foundation();
    new VersionUpdater().update();
  });
})();
