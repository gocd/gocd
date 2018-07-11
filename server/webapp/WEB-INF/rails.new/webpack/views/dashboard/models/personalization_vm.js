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

const _      = require("lodash");
const Stream = require("mithril/stream");

function PersonalizationVM(currentView) {
  const names = Stream([]);
  const dropdown = Stream(false);

  this.names = names;
  this.currentView = currentView;

  this.active = (viewName) => eq(currentView(), viewName);

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
}

/** Case-insensitive functions */
function eq(a, b) { return a.toLowerCase() === b.toLowerCase(); }
function contains(arr, el) { return _.includes(_.map(arr, (a) => a.toLowerCase()), el.toLowerCase()); }

module.exports = PersonalizationVM;
