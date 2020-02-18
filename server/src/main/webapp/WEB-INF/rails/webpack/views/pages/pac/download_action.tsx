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

import {downloadAsFile} from "helpers/download_content_as_file";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {Primary} from "views/components/buttons";
import css from "views/pages/pac/styles.scss";
import {PipelineConfigVMAware} from "views/pages/pipelines/pipeline_config_view_model";
import {ServerErrors} from "views/pages/pipelines/server_errors";

interface Attrs extends PipelineConfigVMAware {
  pluginId: () => string;
}

export class DownloadAction extends MithrilComponent<Attrs> {
  private globalError: Stream<string> = Stream();
  private globalErrorDetail: Stream<Errors> = Stream();

  view(vnode: m.Vnode<Attrs>) {
    return <div class={css.downloadAction}>
      <ServerErrors css={css} message={this.globalError} details={this.globalErrorDetail}/>

      <Primary onclick={this.handleDownload(vnode)}>Download Config</Primary>
    </div>;
  }

  handleDownload(vnode: m.Vnode<Attrs>) {
    const vm = vnode.attrs.vm;
    return (e: MouseEvent) => {
      e.stopPropagation();
      e.preventDefault();

      this.clearErrors();

      if (vm.pipeline.isValid()) {
        vm.preview(vnode.attrs.pluginId(), true).then((result) => {
          result.do((res) => {
            const name = result.header("content-disposition")!.replace(/^attachment; filename=/, "").replace(/^(")(.+)(\1)/, "$2");
            downloadAsFile([res.body], name, result.header("content-type"));
          }, (err) => {
            this.globalError(err.message);
            if (err.body) {
              this.globalErrorDetail(vm.pipeline.consumeErrorsResponse(JSON.parse(err.body).data));
            }
          });
        }).catch((reason) => {
          this.globalError(reason);
        }).finally(m.redraw);
      } else {
        this.globalError("Please fix the validation errors above");
      }
    };
  }

  clearErrors() {
    this.globalError = Stream();
    this.globalErrorDetail = Stream();
  }
}
