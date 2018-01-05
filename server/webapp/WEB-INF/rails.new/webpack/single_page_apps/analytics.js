(function() {
  "use strict";

  const m      = require("mithril");
  const $      = require("jquery");
  require("foundation-sites");

  const VersionUpdater      = require('models/shared/version_updater');

  document.addEventListener("DOMContentLoaded", () => {
    // boilerplate to init menus and check for updates
    $(document).foundation();
    new VersionUpdater().update();
  });
})();
