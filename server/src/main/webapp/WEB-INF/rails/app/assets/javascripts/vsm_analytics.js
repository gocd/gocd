/*
 * Copyright 2020 ThoughtWorks, Inc.
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
(function () {
  "use strict";

  var currentNodeIndex = function currentNodeIndex(vsmGraph) {
    if (vsmGraph.current_material) return 0;

    return _.findIndex(vsmGraph.levels, function (level) {
      return _.find(level.nodes, function (node) {
        return node.id === vsmGraph.current_pipeline;
      });
    });
  };

  var Node = function Node(type, id, index) {
    this.id    = id;
    this.type  = type;
    this.index = index;
  };

  var MaterialNode = function MaterialNode(fingerprint, name, index) {
    Node.call(this, 'material', fingerprint, index);
    this.name = name;
  };

  var PipelineNode = function PipelineNode(name, index) {
    Node.call(this, 'pipeline', name, index);
    this.name = name;
  };

  var currentNode = function currentNode(vsmGraph) {
    if ($j.trim(vsmGraph.current_pipeline)) {
      return new PipelineNode(vsmGraph.current_pipeline, currentNodeIndex(vsmGraph));
    } else {
      return new MaterialNode(vsmGraph.current_material, vsmGraph.levels[0].nodes[0].name, currentNodeIndex(vsmGraph));
    }
  };

  function defineHandlers(chartId) {
    PluginEndpointRequestHandler.defineLinkHandler();
    var models = {};
    models[chartId] = {
      fetch: function fetch(url, handler) {
        var splitURL = url.split('?');
        var search = splitURL[1];
        var jsonData = JSON.parse('{"' + decodeURIComponent(search).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g, '":"') + '"}');
        $j.ajax({
          url: splitURL[0],
          type: "POST",
          dataType: "json",
          data: jsonData
        }).done(function (r) {
          handler(r.data, null);
        });
      }
    };

    PluginEndpointRequestHandler.defineFetchAnalyticsHandler(models);
    PluginEndpoint.ensure("v1");
  }

  function showAnalytics(options) {
    var loadingOverlay = "" +
      "<div class=\"loading-overlay\">" +
      "    <span class=\"page-spinner\"></span>" +
      "    <span class=\"loading-message\">" +
      "        <span class=\"loading-sub\">Loading Analytics</span>" +
      "    </span>" +
      "</div>";

    var vsmModal = document.createElement("div");
    defineHandlers(options.vsmAnalyticsChart.id);
    $j(vsmModal).addClass("vsm_modal");
    $j(vsmModal).append($j(loadingOverlay));
    $j("#analytics-overlay").append(vsmModal);

    $j.ajax({
      url:      options.vsmAnalyticsChart.url,
      dataType: "json",
      data:     options.data,
      type:     options.type || "GET"
    }).done(function (r) {
      var frame     = document.createElement("iframe");
      frame.sandbox = "allow-scripts";
      frame.setAttribute("src", "/go/" + r.view_path);
      vsmModal.appendChild(frame);

      frame.onload = function (_e) {
        PluginEndpoint.init(frame.contentWindow, {
          uid:         options.vsmAnalyticsChart.id,
          pluginId:    options.vsmAnalyticsChart.plugin_id,
          initialData: r.data
        });
      };

    }).fail(function (xhr) {
      if (xhr.getResponseHeader("content-type").indexOf("text/html") !== -1) {
        var frame = document.createElement("iframe");
        frame.src = "data:text/html;charset=utf-8," + xhr.responseText;
        vsmModal.appendChild(frame);
        return;
      }
      var errorEl = document.createElement("div");
      $(errorEl).addClass("error");
      errorEl.textContent = xhr.responseText;
      vsmModal.appendChild(errorEl);
    }).always(function () {
      $j('.loading-overlay').remove();
    });
  }

  var VSMAnalytics = function VSMAnalytics(data, graphRenderer, vsmAnalyticsChart, analyticsPanel, analyticsButton) {
    var self              = this;
    var panel             = analyticsPanel;
    var analyticsButton   = analyticsButton;
    var vsmGraph          = VSMGraph.fromJSON(data);
    var current           = currentNode(vsmGraph);
    var graphRenderer     = graphRenderer;
    var vsmAnalyticsChart = vsmAnalyticsChart;

    var otherNode;

    var source = function source() {
      return current.index < otherNode.index ? current.id : otherNode.id;
    };

    var destination = function destination() {
      return otherNode.index > current.index ? otherNode.id : current.id;
    };

    var renderAnalytics = function renderAnalytics() {
      $j("#analytics-overlay").removeClass("hide");
      $j("body").addClass("noscroll");
      showAnalytics({
        vsmAnalyticsChart: vsmAnalyticsChart,
        type:              'POST',
        data:              self.jsonData()
      });
    };

    this.jsonData = function () {
      return {
        source:      source().toString(),
        destination: destination().toString(),
        vsm_graph:   JSON.stringify(vsmGraph)
      };
    };

    this.init = function () {
      new PrototypeOverrides().overrideJSONStringify();
      graphRenderer.registerSelectPipelineCallback(this.selectPipeline);
      graphRenderer.registerSelectMaterialCallback(this.selectMaterial);

      panel.registerCloseCallback(this.disableAnalytics);
      panel.registerResetCallback(this.resetAnalytics);
      panel.registerViewAnalyticsCallback(renderAnalytics);

      $j(analyticsButton).click(this.enableAnalytics);
    };

    var clearAnalytics = function () {
      $j("#analytics-overlay").addClass("hide");
      $j("#analytics-overlay").empty();
    };

    this.enableAnalytics = function () {
      panel.show();
      graphRenderer.enableAnalyticsMode();
    };

    this.resetAnalytics = function () {
      clearAnalytics();
      graphRenderer.resetAnalyticsMode();
    };

    this.disableAnalytics = function () {
      clearAnalytics();
      graphRenderer.disableAnalyticsMode();
    };

    this.selectPipeline = function (pipelineName, level) {
      if (!pipelineName) return;

      otherNode = new PipelineNode(pipelineName, level);

      if (level < current.index) {
        panel.update(pipelineName, current.name, false);
      } else {
        panel.update(current.name, pipelineName, true);
      }
    };

    this.selectMaterial = function (materialName, fingerprint, level) {
      if ("material" === current.type) return;

      otherNode = new MaterialNode(fingerprint, materialName, level);

      panel.update(materialName, current.name, false);
    };
  };

  if ("undefined" !== typeof module) {
    module.exports = VSMAnalytics;
  } else {
    window.VSMAnalytics = VSMAnalytics;
  }
})();
