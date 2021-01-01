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

import {RoutedSinglePageApp} from "helpers/spa_base";
import m from "mithril";
import {TemplateConfigPage} from "views/pages/template_config";

interface TemplateMeta {
  templateName: string;
}

class RedirectToGeneralTab extends TemplateConfigPage<any> {
  oninit(vnode: m.Vnode<any, any>) {
    const templateName = this.getMeta().templateName;
    m.route.set(templateName + "/general");
  }

  protected getMeta(): TemplateMeta {
    const meta = document.body.getAttribute("data-meta");
    return meta ? JSON.parse(meta) : {};
  }
}

export class TemplateConfigSPA extends RoutedSinglePageApp {
  constructor() {
    super({
            "/": new RedirectToGeneralTab(),
            "/:pipeline_name/:tab_name": new TemplateConfigPage(),
            "/:pipeline_name/:stage_name/:tab_name": new TemplateConfigPage(),
            "/:pipeline_name/:stage_name/:job_name/:tab_name": new TemplateConfigPage()
          });
  }
}

//tslint:disable-next-line
new TemplateConfigSPA();
