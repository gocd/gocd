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
// tslint:disable
/// <reference path="./modal_matchers.d.ts"/>

import _ from "lodash";
import {Modal} from "views/components/modal";
import * as spinnerStyles from "views/components/spinner/index.scss";
import {addMatchers} from "add-matchers";

addMatchers({
  toContainSpinner: (modal: Modal) => {
    return !!document.querySelector(`[data-test-id='${modal.id}'] .${spinnerStyles.pageSpinner}`);
  },
  toContainError: (msg: string, modal: Modal) => {
    // @ts-ignore
    return document.querySelector(`[data-test-id='${modal.id}'] [data-test-id='flash-message-alert']`).innerText.trim() === msg;
  },
  toContainTitle: (title: string, modal: Modal) => {
    // @ts-ignore
    return document.querySelector(`[data-test-id='${modal.id}'] [data-test-id='modal-title']`).innerText.trim() === title;
  },
  toContainBody: (body: string, modal: Modal) => {
    // @ts-ignore
    return document.querySelector(`[data-test-id='${modal.id}'] [data-test-id='modal-body']`).innerText.trim() === body;
  },
  toContainInBody: (body: string, modal: Modal) => {
    // @ts-ignore
    return document.querySelector(`[data-test-id='${modal.id}'] [data-test-id='modal-body']`).innerText.trim().indexOf(body) >= 0;
  },
  toContainSelectorInBody: (selector: string, modal: Modal) => {
    // @ts-ignore
    return !!document.querySelector(`[data-test-id='${modal.id}'] [data-test-id='modal-body'] ${selector}`);
  },
  toContainElementWithDataTestId: (dataTestId: string, modal: Modal) => {
    // @ts-ignore
    return !!document.querySelector(`[data-test-id='${modal.id}'] [data-test-id='modal-body'] [data-test-id=${dataTestId}]`);
  },
  toContainButtons: (expectedButtons: string[], modal: Modal) => {
    const actualButtons = _.map(document.querySelectorAll(`[data-test-id='${modal.id}'] footer *`), (e, i) => {
      // @ts-ignore
      return e.innerText;
    });

    return _.isEqual(actualButtons, expectedButtons);
  }
});
