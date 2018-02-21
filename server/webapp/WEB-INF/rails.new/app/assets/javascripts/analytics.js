(function($) {
  "use strict";

  window.Analytics = {
    modal: function(options) {
      var div = document.createElement("div");

      PluginEndpoint.ensure();
      $(div).addClass("analytics-plugin").dialog({
        title: options.title || "Analytics",
        width: 760,
        height: 495,
        modal: true,
        close: function(e, ui) {
          $(div).remove();
        }
      });

      $.ajax({
        url: options.url,
        dataType: "json",
        type: "GET"
      }).done(function(r) {
        var frame = document.createElement("iframe");
        frame.sandbox = "allow-scripts";

        frame.onload = function(e) {
          PluginEndpoint.init(frame.contentWindow, {data: r.data })
        };

        div.appendChild(frame);
        frame.setAttribute("src", r.view_path);
      }).fail(function(xhr) {
        var errorEl = document.createElement("div");
        $(errorEl).addClass("error");
        errorEl.textContent = xhr.responseText;
        div.appendChild(errorEl);
      });
    }
  };

})(jQuery);
