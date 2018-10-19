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


const extractBoolean = function (body: Element, attribute: string): boolean {
  return JSON.parse(body.getAttribute(attribute) as string);
};

window.addEventListener("DOMContentLoaded", () => {
  const body: Element = document.querySelector('body') as Element;
  let mountPoint  = document.createElement('div');
  body.appendChild(mountPoint);

  const copyrightYear    = body.getAttribute('data-version-copyright-year') as string;
  const goVersion        = body.getAttribute('data-version-go-version') as string;
  const fullVersion      = body.getAttribute('data-version-full') as string;
  const formattedVersion = body.getAttribute('data-version-formatted') as string;

  const showAnalyticsDashboard = extractBoolean(body, 'data-show-analytics-dashboard');
  const canViewAdminPage       = extractBoolean(body, 'data-can-user-view-admin');
  const isUserAdmin            = extractBoolean(body, 'data-is-user-admin');
  const isGroupAdmin           = extractBoolean(body, 'data-is-user-group-admin');
  const canViewTemplates       = extractBoolean(body, 'data-can-user-view-templates');
  const isAnonymous            = extractBoolean(body, 'data-user-anonymous');
  const userDisplayName        = body.getAttribute('data-user-display-name') || "";

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

  const headerData = {
    showAnalyticsDashboard,
    canViewAdminPage,
    isUserAdmin,
    isGroupAdmin,
    canViewTemplates,
    userDisplayName,
    isAnonymous
  };

  m.mount(mountPoint, {
    view() {
      return (
        <MainPage headerData={headerData} footerData={footerData}>
          <PageToMount/>
        </MainPage>
      );
    }
  });
});
