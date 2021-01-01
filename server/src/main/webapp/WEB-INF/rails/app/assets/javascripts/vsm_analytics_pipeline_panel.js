/*
 * Copyright 2021 ThoughtWorks, Inc.
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
    var selectPipelines = "#select-pipelines-template";
    var viewAnalyticsButton = ".view-vsm-analytics";
    var resetAnalyticsButton = ".reset-vsm-analytics";

    var closeCallback = void 0;
    var resetCallback = void 0;
    var viewAnalyticsCallback = void 0;

    var viewAnalytics = function viewAnalytics() {
      if (!$j(viewAnalyticsButton).hasClass("disabled")) {
        $j(container).find(resetAnalyticsButton).removeClass("hide");
        $j(container).find(viewAnalyticsButton).addClass("hide");
        viewAnalyticsCallback();
      }
    };

    var resetAnalytics = function resetAnalytics() {
      $j(container).find(resetAnalyticsButton).addClass("hide");
      $j(container).find(viewAnalyticsButton).removeClass("hide");
      disableViewAnalytics();
      self.reset();
    };

    var init = function init() {
      $j(container).find(".analytics-close").click(self.hide);
      $j(container).find(resetAnalyticsButton).click(resetAnalytics);
      $j(container).find(viewAnalyticsButton).click(viewAnalytics);
    };

    this.show = function () {
      $j(container).show();
    };

    var showSelected = function showSelected() {
      $j(selectedPipelines).removeClass("hide");
      $j(selectPipelines).addClass("hide");
      enableViewAnalytics();
    };

    var showSelectionTemplate = function showSelectionTemplate() {
      $j(selectPipelines).removeClass("hide");
      $j(selectedPipelines).addClass("hide");
    };

    var enableViewAnalytics = function enableViewAnalytics() {
      $j(viewAnalyticsButton).removeClass("disabled");
    };

    var disableViewAnalytics = function disableViewAnalytics() {
      $j(viewAnalyticsButton).addClass("disabled");
    };

    var updateSource = function (src, isCurrent) {
      var source = $j(selectedPipelines).find(".analytics-source");

      source.removeClass("analytics-current");
      source.removeClass("analytics-other");
      source.text(src);

      isCurrent ? source.addClass("analytics-current") : source.addClass("analytics-other");
    };

    var updateDestination = function (dest, isCurrent) {
      var destination = $j(selectedPipelines).find(".analytics-destination");

      destination.removeClass("analytics-current");
      destination.removeClass("analytics-other");
      destination.text(dest);

      isCurrent ? destination.addClass("analytics-current") : destination.addClass("analytics-other");
    };

    this.reset = function () {
      showSelectionTemplate();
      disableViewAnalytics();
      resetCallback();

      $j(selectedPipelines).find(".analytics-source").text("");
      $j(selectedPipelines).find(".analytics-destination").text("");
    };

    this.hide = function () {
      $j(container).hide();
      resetAnalytics();
      closeCallback();
    };

    this.update = function (source, destination, isSourceCurrent) {
      updateSource(source, isSourceCurrent);
      updateDestination(destination, !isSourceCurrent);

      showSelected();
    };

    this.registerCloseCallback = function (callback) {
      closeCallback = callback;
    };

    this.registerResetCallback = function (callback) {
      resetCallback = callback;
    };

    this.registerViewAnalyticsCallback = function (callback) {
      viewAnalyticsCallback = callback;
    };

    init();
  };

  if ("undefined" !== typeof module) {
    module.exports = VSMAnalyticsPipelinePanel;
  } else {
    window.VSMAnalyticsPipelinePanel = VSMAnalyticsPipelinePanel;
  }
})();