var Analytics = {
  modal: function(data){
    jQuery.ajax({
                  url: data.url,
                  params: {
                    pipeline_counter: data.pipeline_counter
                  },
                  success: function(r) {
                    var div = document.createElement("div");
                    var frame = document.createElement("iframe");
                    frame.setAttribute("id", "analytics-frame");
                    frame.setAttribute("src", r.view_path);
                    frame.width="100%";
                    frame.height="100%";
                    frame.setAttribute("scrolling", "no");
                    frame.setStyle("position: absolute; border: none; margin: 0; padding: 0;");
                    frame.sandbox = "allow-scripts";
                    div.appendChild(frame);
                    var options = {
                      "autoFocusing": false,
                      "height": 495,
                      "onShow": function() {
                          var frame = document.getElementById("analytics-frame");
                          frame.onload = function() {
                            var x = { data: JSON.parse(r.data)};
                            frame.contentWindow.postMessage(x, "*");
                          }
                      }
                    };
                    Modalbox.show(div, options);
                  }
    });

  }
}
