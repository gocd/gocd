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

  document.addEventListener("DOMContentLoaded", () => {
    const main = document.getElementById("analytics-container");

  //loop plugin ids (data attribute), mount iframe widget, make call to dashboard (return viewpath + data), send message to iframe with data, set src to viewpath
    m.mount(main, {
      view() {
        return $.map($(main).data("plugin-ids"), (id) => {
          return m(PluginiFrameWidget, {url: Routes.dashboardAnalyticsPath({plugin_id: id})})
        });
      }
    })

    // boilerplate to init menus and check for updates
    $(document).foundation();
    new VersionUpdater().update();
  });
})();
