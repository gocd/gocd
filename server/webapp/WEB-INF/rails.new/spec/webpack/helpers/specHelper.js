/*
 * Copyright 2017 ThoughtWorks, Inc.
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

require('jasmine-jquery');
require('jasmine-ajax');

var Environments  = require('models/agents/environments');
var Resources     = require('models/agents/resources');
var PluginInfos   = require('models/pipeline_configs/plugin_infos');
var PluggableSCMs = require('models/pipeline_configs/pluggable_scms');
var SCMs          = require("models/pipeline_configs/scms");
var Pipelines     = require("models/pipeline_configs/pipelines");
var _             = require('lodash');
var $             = require('jquery');
var Modal         = require('views/shared/new_modal');

var container;

window.createDomElementForTest = function () {
  container = $('<div>');

  if (window.location.search.indexOf('showDom=true') === 0) {
    container.hide();
  }

  var mithrilMountPoint = $('<div>').attr({class: 'mithril-mount-point'});
  container.append(mithrilMountPoint);
  $('body').append(container);

  return [mithrilMountPoint, mithrilMountPoint.get(0)];
};

window.destroyDomElementForTest = function () {
  container.remove();
};

beforeEach(function () {
  if ($('#mithril-component-container').length === 0) {
    var container = $('<div>').attr({id: 'mithril-component-container'}).hide();
    container.append($('<div>').attr({id: 'mithril-mount-point'}));
    $('body').append(container);
  }
});

afterEach(function () {
  expect(Environments.list.length).toBe(0);
  expect(Resources.list.length).toBe(0);
  expect(PluginInfos().length).toBe(0);
  expect(Pipelines().length).toBe(0);

  expect(_(PluggableSCMs.Types).isEqual({})).toBe(true);

  expect(SCMs().length).toBe(0);
  expect(_(SCMs.scmIdToEtag).isEqual({})).toBe(true);

  expect($('#mithril-mount-point').html()).toEqual('');
  expect(Modal.count()).toBe(0);
});