/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import m from "mithril";
import stream from "mithril/stream";
import {BuilderForm} from "views/pages/pac/builder_form";
import {DownloadAction} from "views/pages/pac/download_action";
import {PreviewPane} from "views/pages/pac/preview_pane";
import {Page, PageState} from "views/pages/page";
import {FillableSection} from "views/pages/pipelines/fillable_section";
import {PipelineConfigVM} from "views/pages/pipelines/pipeline_config_view_model";

export class PipelinesAsCodeCreatePage extends Page {
  private model = new PipelineConfigVM();
  private content = stream("");
  private mimeType = stream("application/x-yaml");

  oninit(vnode: m.Vnode) {
    this.pageState = PageState.OK;
    const group = m.parseQueryString(window.location.search).group;
    if ("" !== String(group || "").trim()) {
      this.model.pipeline.group(group);
    }
  }

  pageName() {
    return "Add a New Pipeline as Code";
  }

  componentToDisplay(vnode: m.Vnode) {
    const vm = this.model;

    return [
      <FillableSection>
        <BuilderForm vm={vm} onContentChange={(updated) => {
          if (updated) {
            vm.preview(vm.pluginId()).then((result) => {
              if (304 === result.getStatusCode()) {
                return;
              }

              result.do((res) => {
                this.content(res.body);
              }, (err) => console.error(err)); // tslint:disable-line no-console
            });
          }
        }}/>

        <PreviewPane content={this.content} mimeType={this.mimeType}/>
      </FillableSection>,

      <FillableSection>
        <div>Download this as a file and put it in your repo (I need some proper copy here).</div>

        <DownloadAction vm={vm}/>
      </FillableSection>
    ];
  }

  fetchData() { return new Promise(() => null); }
}
