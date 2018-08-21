/*
 * Copyright 2018 ThoughtWorks, Inc.
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

  var VSMAnalyticsPipelinePanel = function VSMAnalyticsPipelinePanel(cont) {
    var self = this;
    var container = cont;
    var selectedPipelines = "#selected-pipelines";
    var selectPipelines = "#select-pipelines";
    var viewAnalyticsButton = ".view-vsm-analytics";

    var closeCallback = void 0;

    var init = function init() {
      $j(container).find(".analytics-close").click(self.hide);
    };

    this.show = function () {
      $j(container).show();
    };

    var showSelected = function showSelected() {
      $j(selectedPipelines).removeClass("hide");
      $j(selectPipelines).addClass("hide");
      enableViewAnalytics();
    };

    var showSelectionInfo = function showSelectionInfo() {
      $j(selectPipelines).removeClass("hide");
      $j(selectedPipelines).addClass("hide");
    };

    var enableViewAnalytics = function enableViewAnalytics() {
      $j(viewAnalyticsButton).removeClass("disabled");
    };

    var disableViewAnalytics = function disableViewAnalytics() {
      $j(viewAnalyticsButton).addClass("disabled");
    };

    this.reset = function () {
      showSelectionInfo();
      disableViewAnalytics();
      $j(selectedPipelines).find(".analytics_downstream").show();
      $j(selectedPipelines).find(".analytics-upstream .selected-name").text("");
      $j(selectedPipelines).find(".analytics-upstream").show();
      $j(selectedPipelines).find(".analytics-downstream .selected-name").text("");
    };

    this.hide = function () {
      $j(container).hide();
      closeCallback();
      self.reset();
    };

    this.upstream = function (pipelineName) {
      $j(selectedPipelines).find(".analytics-downstream").hide();
      $j(selectedPipelines).find(".analytics-upstream .selected-name").text(pipelineName);
      $j(selectedPipelines).find(".analytics-upstream").show();
      showSelected();
    };

    this.downstream = function (pipelineName) {
      $j(selectedPipelines).find(".analytics-upstream").hide();
      $j(selectedPipelines).find(".analytics-downstream .selected-name").text(pipelineName);
      $j(selectedPipelines).find(".analytics-downstream").show();
      showSelected();
    };

    this.material = function (materialName) {
      self.upstream(materialName);
    };

    this.registerCloseCallback = function (callback) {
      closeCallback = callback;
    };

    this.registerViewAnalyticsCallback = function (callback) {
      $j(container).find(".view-vsm-analytics").click(callback);
    };

    init();
  };

  if ("undefined" !== typeof module) {
    module.exports = VSMAnalyticsPipelinePanel;
  } else {
    window.VSMAnalyticsPipelinePanel = VSMAnalyticsPipelinePanel;
  }
})();