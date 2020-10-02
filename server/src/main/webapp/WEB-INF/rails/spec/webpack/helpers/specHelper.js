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

import {ModalManager} from "views/components/modal/modal_manager";

import {Modal} from "views/shared/new_modal";
import "jasmine-ajax";
import "./dom_matchers";

jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000;

document.addEventListener('DOMContentLoaded', () => {
  ModalManager.onPageLoad();
});

//function blowUpAjaxFunction() {
//  fail("Ajax calls need to be stubbed!"); //eslint-disable-line no-undef
//}

//const realAjaxFunction = window.XMLHttpRequest;
//
//beforeEach(() => {
//  if (window.XMLHttpRequest !== blowUpAjaxFunction) {
//    window.XMLHttpRequest = blowUpAjaxFunction;
//  }
//});
//
//afterEach(() => {
//  window.XMLHttpRequest = realAjaxFunction;
//});


beforeEach(() => {
  expect(jasmine.Ajax.requests.count()).toBe(0);
});

afterEach(() => {
  expect(jasmine.Ajax.requests.count()).toBe(0);
});

afterEach(() => {
  expect(Modal.count()).toBe(0);
});
