(function($) {
  "use strict";

  window.Analytics = {
    modal: function(data) {
      var div = document.createElement("div");

      ExtensionIFrameEndpoint.ensure();

      $(div).addClass("analytics-plugin").dialog({
        title: data.title || "Analytics",
        width: 760,
        height: 495,
        close: function(e, ui) {
          $(div).remove();
        }
      });

      $.ajax({
        url: data.url,
        params: {
          pipeline_counter: data.pipeline_counter
        },
        dataType: "json",
        type: "GET"
      }).done(function(r) {
        var frame = document.createElement("iframe");
        frame.sandbox = "allow-scripts";

        frame.onload = function(e) {
          ExtensionIFrameEndpoint.send(frame.contentWindow, "analytics.pipeline-chart", JSON.parse(r.data));
        };

        div.appendChild(frame);
        frame.setAttribute("src", r.view_path);
      });
    }
  };

})(jQuery);
