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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from 'lodash';
import m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {EnvironmentVariablesWidget} from "views/components/environment_variables";
import {AngleDoubleRight} from "views/components/icons";
import {Link} from "views/components/link";
import {Tabs} from "views/components/tab";
import style from "views/pages/clicky_pipeline_config/index.scss";
import {ChangeRouteEvent} from "views/pages/clicky_pipeline_config/pipeline_config";
import {GeneralOptionsTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/general_options_tab";
import {MaterialsTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/materials_tab";
import {ParametersTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/parameters_tab";
import {ProjectManagementTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/project_management_tab";
import {StagesTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/stages_tab";
import {TabWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/tab_widget";

interface Attrs {
  pipelineConfig: PipelineConfig;
  templateConfig: TemplateConfig;

  changeRoute(event: ChangeRouteEvent, success: () => void): void;
}

export class EnvironmentVariablesTab extends TabWidget {

  name(): string {
    return "Environment Variables";
  }

  protected renderer(entity: PipelineConfig, templateConfig: TemplateConfig): m.Children {
    return <EnvironmentVariablesWidget environmentVariables={entity.environmentVariables()}/>;
  }
}

const tabs = [
  new GeneralOptionsTab(),
  new EnvironmentVariablesTab(),
  new ProjectManagementTab(),
  new MaterialsTab(),
  new StagesTab(),
  new ParametersTab()
];

export class PipelineConfigWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    this.headerPanel();
    return [
      <div class={style.steps}>{this.headerPanel()}</div>,
      <Tabs initialSelection={this.selectedTabIndex()}
            tabs={tabs.map((eachTab: TabWidget) => eachTab.name())}
            contents={tabs.map((eachTab: TabWidget) => eachTab.content(vnode.attrs.pipelineConfig, vnode.attrs.templateConfig, this.isSelectedTab(eachTab)))}
            beforeChange={this.onTabChange.bind(this, vnode)}/>
    ];
  }

  private onTabChange(vnode: m.Vnode<Attrs>, index: number, callback: () => void) {
    const route = `${vnode.attrs.pipelineConfig.name()}/${_.snakeCase(tabs[index].name())}`;
    vnode.attrs.changeRoute({newRoute: route}, () => {
      callback();
      if (m.route.get() !== route) {
        m.route.set(route);
      }
    });
  }

  private selectedTabIndex() {
    return tabs.findIndex((eachTab) => {
      return this.isSelectedTab(eachTab);
    });
  }

  private isSelectedTab(eachTab: TabWidget) {
    return _.snakeCase(eachTab.name()) === m.route.param().tab_name;
  }

  private headerPanel() {
    const params = m.route.param();
    if (params.job_name) {
      return [
        <Link onclick={() => m.route.set(`${params.pipeline_name}/${params.tab_name}`)}>{params.pipeline_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <Link
          onclick={() => m.route.set(`${params.pipeline_name}/${params.stage_name}/${params.tab_name}`)}>{params.stage_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <label>{params.job_name}</label>
      ];
    }

    if (params.stage_name) {
      return [
        <Link onclick={() => m.route.set(`${params.pipeline_name}/${params.tab_name}`)}>{params.pipeline_name}</Link>,
        <AngleDoubleRight iconOnly={true}/>,
        <label>{params.stage_name}</label>
      ];
    }

    if (params.pipeline_name) {
      return [<label>{params.pipeline_name}</label>];
    }
  }
}
