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

import _ from "lodash";
import m from "mithril";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Tab, Tabs} from "models/pipeline_configs/tab";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {Secondary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HelpText, TextField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import styles from "./custom_tabs.scss";

export class CustomTabTabContent extends TabContent<Job> {

  static tabName(): string {
    return "Custom Tabs";
  }

  protected renderer(entity: Job, templateConfig: TemplateConfig): m.Children {
    const msg     = "Custom Tabs lets you add new tabs within the Job Details page.";
    const docLink = <HelpText helpText=" "
                              docLink="faq/dev_see_artifact_as_tab.html"
                              helpTextId={`custom-tab-doc-link`}/>;

    const flashMsg = <FlashMessage type={MessageType.info}>{msg} {docLink}</FlashMessage>;
    return (<div class={styles.mainContainer} data-test-id="custom-tabs">
      {flashMsg}
      {this.getTabView(entity.tabs())}
      {this.getAddTabBtn(entity.tabs())}
    </div>);
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Job {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!.jobs().findByName(routeParams.job_name!)!;
  }

  private addEmptyTab(tabs: Tabs) {
    const tab = new Tab("", "");
    return tabs.push(tab);
  }

  private getAddTabBtn(tabs: Tabs) {
    return (<Secondary small={true} dataTestId={"add-custom-tab-button"} onclick={this.addEmptyTab.bind(null, tabs)}>+ Add</Secondary>);
  }

  private getTabView(tabs: Tabs) {
    const tabsHeader = (<div class={styles.tabsHeader} data-test-id="tabs-header">
      <span data-test-id="name-header">
        Tab Name: <Tooltip.Info size={TooltipSize.small}
                                content={"Name of the tab which will appear in the Job Detail Page."}/>
      </span>
      <span data-test-id="path-header">
        Path: <Tooltip.Info size={TooltipSize.small}
                            content={"The artifact that will be rendered in the custom tab. This is typically a html or xml file."}/>
      </span>
    </div>);

    if (tabs.length === 0) {
      this.addEmptyTab(tabs);
    }

    const tabsView = tabs.map((tab, index) => {
      return (<div class={styles.tabContainer} data-test-id={`tab-${index}`}>
        <TextField dataTestId={`tab-name-${tab.name()}`}
                   errorText={tab.errors().errorsForDisplay("name")}
                   placeholder="name" property={tab.name}/>
        <TextField dataTestId={`tab-path-${tab.path()}`}
                   errorText={tab.errors().errorsForDisplay("path")}
                   placeholder="path"
                   property={tab.path}/>
        <Icons.Close data-test-id={`remove-tab-${tab.name()}`}
                     iconOnly={true}
                     onclick={() => this.removeEntity(tab, tabs)}/>
      </div>);
    });

    return <div data-test-id="tabs-container">
      {tabsHeader}
      {tabsView}
    </div>;
  }

  private removeEntity(entityToRemove: Tab, collection: Tabs) {
    _.remove(collection, (t) => t.name() === entityToRemove.name() && t.path() === entityToRemove.path());
  }
}
