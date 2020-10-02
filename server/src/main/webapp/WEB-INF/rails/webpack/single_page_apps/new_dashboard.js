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
import $ from "jquery";
import m from "mithril";
import Stream from "mithril/stream";
import {DashboardViewModel as DashboardVM} from "views/dashboard/models/dashboard_view_model";
import {Dashboard} from "models/dashboard/dashboard";
import {DashboardWidget} from "views/dashboard/dashboard_widget";
import {AjaxPoller} from "helpers/ajax_poller";
import {PageLoadError} from "views/shared/page_load_error";
import {PersonalizationVM as PersonalizeVM} from "views/dashboard/models/personalization_vm";
import {PluginInfos} from "../models/shared/plugin_infos";

$(() => {
  const dashboardElem              = $('#dashboard');
  const showEmptyPipelineGroups    = JSON.parse(dashboardElem.attr('data-show-empty-pipeline-groups'));
  const shouldShowAnalyticsIcon    = JSON.parse(dashboardElem.attr('data-should-show-analytics-icon'));
  const testDrive                  = JSON.parse(dashboardElem.attr('data-test-drive'));
  const pluginsSupportingAnalytics = {};

  const dashboard     = new Dashboard();
  const dashboardVM   = new DashboardVM(dashboard);
  const personalizeVM = new PersonalizeVM(currentView);

  function queryObject() {
    return m.parseQueryString(window.location.search);
  }

  /**
   * A Stream-like function that manages the viewName state in the URL via the history API
   * in order to facilitate tab switches without a full page load.
   *
   * Called with no arguments, this method returns the current view name; called with 1
   * argument, it sets the new value as the current view (and forces the page to reflect
   * that change).
   *
   * This API is built so that callers of this function need no knowledge of the current
   * mithril route. The current route is preserved, and viewName can be independently set
   * to make bookmarkable, stateful URLs.
   *
   * The differences from Stream are that 1) this really only works with String values
   * and 2) one cannot use this in Stream.combine/merge/map because it cannot be registered
   * as a dependency to another Stream.
   *
   * That said, it's worth noting that this *could* be equivalently implemented as a genuine
   * Stream that is dependent on a vanilla Stream instance (by way of Sream.map or
   * Stream.combine), but there's no practical benefit to this.
   *
   * Here's how it would look anyway:
   *
   * ```
   * const viewName = Stream(queryObject().viewName);
   * const currentView = viewName.map(function onUpdateValue(val) {
   *   // logic to build new URL and handle history, routing, etc
   *   // goes here.
   * });
   * ```
   */
  function currentView(viewName, replace = false) {
    const current = queryObject();
    if (!arguments.length) {
      return current.viewName || personalizeVM.names()[0];
    }

    const route = window.location.hash.replace(/^#!/, "");

    if (current.viewName !== viewName) {
      const path = [
        window.location.pathname,
        viewName ? `?${m.buildQueryString($.extend({}, current, {viewName}))}` : "",
        route ? `#!${route}` : ""
      ].join("");

      history[replace ? "replaceState" : "pushState"](null, "", path);

      if (route) {
        m.route.set(route, null, {replace: true}); // force m.route() evaluation
      }
    }

    personalizeVM.loadingView(true);

    // Explicit set always refreshes; even if the viewName didn't change,
    // we should refresh because the filter definition may have changed as
    // currentView() is called after every personalization save operation.
    repeater().restart();
  }

  $(document.body).on("click", () => {
    dashboardVM.dropdown.hide();
    dashboardVM.stageOverview.hide();
    personalizeVM.hideAllDropdowns();

    /**
     * The reason we need to do the redraw asynchronously is for checkboxes. When you click
     * a checkbox the click event propogates to the body. But the model backing the checkboxes
     * has not had time to update to the new value (so the redraw overrides the value with the
     * original state). Using setTimeout() to make m.redraw() asynchronous allows mithril to
     * flush the new checkbox state to the DOM beforehand.
     */
    setTimeout(m.redraw, 0);
  });

  $(window).on("popstate", () => {
    personalizeVM.activate(currentView());
  });

  function onResponse(dashboardData, message = undefined) {
    personalizeVM.etag(dashboardData['_personalization']);
    dashboard.initialize(dashboardData, showEmptyPipelineGroups);
    dashboard.message(message);
  }

  function parseEtag(req) {
    return (req.getResponseHeader("ETag") || "").replace(/"/g, "").replace(/--(gzip|deflate)$/, "");
  }

  function createRepeater() {
    const onsuccess = (data, _textStatus, jqXHR) => {
      const etag = parseEtag(jqXHR);

      if (jqXHR.status === 304) {
        return;
      }

      if (jqXHR.status === 202) {
        const message = {
          type:    "info",
          content: data.message
        };
        onResponse({}, message);
        return;
      }

      if (etag) {
        dashboardVM.etag(etag);
      }

      onResponse(data);
    };

    const onerror = (jqXHR, textStatus, errorThrown) => {
      // fix for issue #5391
      //forcefully remove the ETag if server backup is in progress,
      //so that on next dashboard request, the server will send a 200 and re-render the page
      if (jqXHR.status === 503) {
        dashboardVM.etag(null);
      }

      if (textStatus === 'parsererror') {
        const message = {
          type:    "alert",
          content: "Error occurred while parsing dashboard API response. Check server logs for more information."
        };
        onResponse({}, message);
        console.error(errorThrown); // eslint-disable-line no-console
        return;
      }

      if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
        const message = {
          type:    'warning',
          content: jqXHR.responseJSON.message,
        };
        onResponse({}, message);
        return;
      }

      const message = {
        type:    'warning',
        content: 'There was an unknown error ',
      };
      onResponse({}, message);
    };

    return new AjaxPoller(() => Dashboard.get(currentView(), dashboardVM.etag(), true)
      .then(onsuccess, onerror)
      .always(() => {
        personalizeVM.loadingView(false);
        showSpinner(false);
      }));
  }

  const repeater    = Stream(createRepeater());
  const showSpinner = Stream(true);

  const renderView = () => {
    const component = {
      view() {
        return m(DashboardWidget, {
          personalizeVM,
          showSpinner,
          pluginsSupportingAnalytics,
          shouldShowAnalyticsIcon,
          testDrive,
          vm:                   dashboardVM,
          doCancelPolling:      () => repeater().stop(),
          doRefreshImmediately: () => repeater().restart()
        });
      }
    };

    m.route(dashboardElem.get(0), '', {
      '/':            component,
      '/:searchedBy': component
    });

    dashboardVM.searchText(m.route.param('searchedBy') || '');
  };

  const onInitialAPIsResponse = (responses) => {
    const pluginInfos = responses[1];

    pluginInfos.eachPluginInfo((pluginInfo) => {
      const supportedPipelineAnalytics = pluginInfo.extensions().analytics.capabilities().supportedPipelineAnalytics();
      if (supportedPipelineAnalytics.length > 0) {
        pluginsSupportingAnalytics[pluginInfo.id()] = supportedPipelineAnalytics[0].id;
      }
    });

    renderView();
    repeater().start();
  };

  const onInitialAPIsFailure = (response) => {

    m.mount(dashboardElem.get(0), {
      view() {
        return (<PageLoadError message={response}/>);
      }
    });
  };

  Promise.all([personalizeVM.fetchPersonalization(), PluginInfos.all(null, {type: 'analytics'})]).then(onInitialAPIsResponse, onInitialAPIsFailure);
});
