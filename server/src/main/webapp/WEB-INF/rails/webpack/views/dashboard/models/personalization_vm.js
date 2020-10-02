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
import _ from "lodash";
import Stream from "mithril/stream";
import {Personalization} from "models/dashboard/personalization";
import {DashboardFilter} from "../../../models/dashboard/dashboard_filter";

export function PersonalizationVM(currentView) {
  const names             = Stream([]);
  const tabSettingsDD     = Stream(false);
  const tabsListDD        = Stream(false);
  const checksum          = Stream();
  const pipelinesChecksum = Stream();
  const model             = Stream();
  const errorMessage      = Stream();
  const loadingView       = Stream(false);

  const paged        = Stream(false); // flag indicating whether the view tabs need scrollable behavior
  const currentVnode = Stream(); // handle on current tab's vnode; allows sharing state between components
  const stagedSort   = Stream(null);
  const actionPopup  = Stream(null);

  let requestPending, tick;

  function fetchPersonalization() {
    return Personalization.get(checksum()).then((personalization, xhr) => {
      if (304 !== xhr.status) {
        checksum(parseEtag(xhr));

        names(personalization.names());
        model(personalization);
      }
      requestPending = false;
      return personalization;
    });
  }

  function checkForUpdates(etag) {
    if (!arguments.length) {
      return checksum();
    }

    if (requestPending) {
      if ("number" === typeof tick) {
        tick = clearTimeout(tick);
      } // only allow 1 queued request
      tick = setTimeout(() => checkForUpdates(etag), 100);
      return;
    }

    if (!checksum() || etag !== checksum()) {
      requestPending = true;
      fetchPersonalization();
    }
  }

  const changeListeners = [];

  _.assign(this, {
    model,
    names,
    currentView,
    fetchPersonalization,
    etag: checkForUpdates,
    checksum,
    errorMessage,
    loadingView,
    paged,
    currentVnode,
    stagedSort,
    actionPopup,
    pipelinesChecksum
  });

  this.tabs = () => _.map(stagedSort() ? stagedSort().names() : names(), (name) => {
    return {id: name, name};
  });

  this.updatePipelineGroups = () => {
    return Personalization.getPipelines(pipelinesChecksum()).then((data, _textStatus, xhr) => {
      if (304 !== xhr.status) {
        model().pipelineGroups(data.pipelines);
        pipelinesChecksum(parseEtag(xhr));
      }
    });
  };

  this.locked = () => !!stagedSort();

  this.canonicalCurrentName = () => _.find(names(), (n) => eq(n, currentView()));

  this.active = (viewName) => eq(currentView(), viewName);

  this.isDefault = (viewName) => eq(viewName, "Default");

  this.selectFirstView = () => {
    currentView(names()[0]);
  };

  this.activate = (viewName) => {
    currentView(contains(names(), viewName) ? viewName : "Default");
    tabSettingsDD(false);
    this.onchange();
  };

  this.tabSettingsDropdownVisible = () => tabSettingsDD();

  this.toggleTabSettingsDropdown = () => {
    tabsListDD(false);
    tabSettingsDD(!tabSettingsDD());
  };

  this.hideAllDropdowns = () => {
    tabSettingsDD(false);
    tabsListDD(false);
  };

  this.tabsListDropdownVisible = () => tabsListDD();

  this.toggleTabsListDropdown = () => {
    tabSettingsDD(false);
    tabsListDD(!tabsListDD());
  };

  this.actionHandler = (fn) => {
    return (e) => {
      e.stopPropagation();
      this.hideAllDropdowns();
      fn();
    };
  };

  this.onchange = function registerOrExec(fn) {
    if (!arguments.length) {
      _.each(changeListeners, (fn) => fn());
      return;
    }

    if ("function" !== typeof fn || _.includes(changeListeners, fn)) {
      return;
    }

    changeListeners.push(fn);
  };

  this.currentFilter = () => {
    return new DashboardFilter(this.model().namedFilter(this.currentView()));
  };
}

function parseEtag(req) {
  return (req.getResponseHeader("ETag") || "").replace(/"/g, "").replace(/--(gzip|deflate)$/, "");
}

/** Case-insensitive functions */
function eq(a, b) {
  return a.toLowerCase() === b.toLowerCase();
}

function contains(arr, el) {
  return _.includes(_.map(arr, (a) => a.toLowerCase()), el.toLowerCase());
}
