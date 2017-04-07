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

const SCMs  = require("models/pipeline_configs/scms");
const _     = require('lodash');
const $     = require('jquery');
const Modal = require('views/shared/new_modal');

let container;

window.createDomElementForTest = function () {
  container = $('<div>');

  if (window.location.search.indexOf('showDom=true') === 0) {
    container.hide();
  }

  const mithrilMountPoint = $('<div>').attr({class: 'mithril-mount-point'});
  container.append(mithrilMountPoint);
  $('body').append(container);

  return [mithrilMountPoint, mithrilMountPoint.get(0)];
};

window.destroyDomElementForTest = function () {
  container.remove();
};

beforeEach(() => {
  if ($('#mithril-component-container').length === 0) {
    const container = $('<div>').attr({id: 'mithril-component-container'}).hide();
    container.append($('<div>').attr({id: 'mithril-mount-point'}));
    $('body').append(container);
  }
});

afterEach(() => {
  expect(SCMs().length).toBe(0);
  expect(_(SCMs.scmIdToEtag).isEqual({})).toBe(true);

  expect($('#mithril-mount-point').html()).toEqual('');
  expect(Modal.count()).toBe(0);
});
