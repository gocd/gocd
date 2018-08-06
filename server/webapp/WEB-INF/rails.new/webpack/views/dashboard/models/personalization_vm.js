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

const _               = require("lodash");
const Stream          = require("mithril/stream");
const Personalization = require("models/dashboard/personalization");

function PersonalizationVM(currentView) {
  const names        = Stream([]);
  const dropdown     = Stream(false);
  const checksum     = Stream();
  const model        = Stream();
  const errorMessage = Stream();

  const paged        = Stream(false); // flag indicating whether the view tabs need scrollable behavior
  const currentVnode = Stream(); // handle on current tab's vnode; allows sharing state between components
  const stagedSort   = Stream(null);
  const actionPopup  = Stream(null);

  let requestPending, tick;

  function checkForUpdates(etag) {
    if (!arguments.length) { return checksum(); }

    if (requestPending) {
      if ("number" === typeof tick) { tick = clearTimeout(tick); } // only allow 1 queued request
      tick = setTimeout(() => checkForUpdates(etag), 100);
      return;
    }

    if (!checksum() || etag !== checksum()) {
      requestPending = true;
      Personalization.get(checksum()).then((personalization, xhr) => {
        if (304 !== xhr.status) {
          checksum(parseEtag(xhr));

          names(personalization.names());
          model(personalization);
        }
        requestPending = false;
      });
    }
  }

  const changeListeners = [];

  _.assign(this, {model, names, currentView, etag: checkForUpdates, checksum, errorMessage, paged, currentVnode, stagedSort, actionPopup});

  this.tabs = () => _.map(stagedSort() ? stagedSort().names() : names(), (name) => { return {id: name, name}; });

  this.locked = () => !!stagedSort();

  this.canonicalCurrentName = () => _.find(names(), (n) => eq(n, currentView()));

  this.active = (viewName) => eq(currentView(), viewName);

  this.isDefault = (viewName) => eq(viewName, "Default");

  this.activate = (viewName) => {
    currentView(contains(names(), viewName) ? viewName : "Default");
    dropdown(false);
  };

  this.dropdownVisible = () => dropdown();

  this.toggleDropdown = () => {
    dropdown(!dropdown());
  };

  this.hideDropdown = () => {
    dropdown(false);
  };

  this.actionHandler = (fn) => {
    return (e) => { e.stopPropagation(); dropdown(false); fn(); };
  };

  this.onchange = function registerOrExec(fn) {
    if (!arguments.length) {
      _.each(changeListeners, (fn) => fn());
      return;
    }

    if ("function" !== typeof fn || _.includes(changeListeners, fn)) { return; }

    changeListeners.push(fn);
  };
}

function parseEtag(req) { return (req.getResponseHeader("ETag") || "").replace(/"/g, "").replace(/--(gzip|deflate)$/, ""); }

/** Case-insensitive functions */
function eq(a, b) { return a.toLowerCase() === b.toLowerCase(); }
function contains(arr, el) { return _.includes(_.map(arr, (a) => a.toLowerCase()), el.toLowerCase()); }

module.exports = PersonalizationVM;
