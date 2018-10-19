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

import * as m from 'mithril';

import {MainPage} from "../views/pages/main";

window.addEventListener("DOMContentLoaded", () => {
  const body: Element = document.querySelector('body') as Element;

  const copyrightYear    = body.getAttribute('data-version-copyright-year') as string;
  const goVersion        = body.getAttribute('data-version-go-version') as string;
  const fullVersion      = body.getAttribute('data-version-full') as string;
  const formattedVersion = body.getAttribute('data-version-formatted') as string;

  const pageName = body.getAttribute('data-controller-name');

  let PageToMount: any;
  try {
    PageToMount = require(`views/pages/${pageName}.tsx`);
  } catch (e) {
    PageToMount = require(`views/pages/${pageName}.js`);
  }

  const footerData = {
    copyrightYear, goVersion, fullVersion, formattedVersion
  };

  m.mount(body, {
    view() {
      return (
        <MainPage footerData={footerData}>
          <PageToMount/>
        </MainPage>
      );
    }
  });
});
