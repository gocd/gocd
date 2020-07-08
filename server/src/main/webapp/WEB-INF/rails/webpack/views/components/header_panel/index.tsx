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
import _ from "lodash";
import m from "mithril";
import {HeaderKeyValuePair} from "views/components/header_panel/header_key_value_pair";
import {QuestionCircle} from "views/components/icons";
import {Modal} from "views/components/modal";
import * as style from "./index.scss";

export interface Attrs {
  title: m.Children;
  sectionName?: m.Children;
  buttons?: m.Children;
  keyValuePair?: { [key: string]: m.Children };
  help?: m.Children;
}

export class HeaderPanel extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let buttons: m.Children = null;
    const onclick = (e: MouseEvent) => {
      e.stopPropagation();
      new HelpTextModal(vnode.attrs.help).render();
    };

    const helpText: m.Children = vnode.attrs.help
      ? (<span class={style.helpTextWrapper} data-test-id="help-text-icon">
      <QuestionCircle iconOnly={true} title={"Show Help Text"} onclick={onclick}/>
    </span>)
      : undefined;

    if (!_.isEmpty(vnode.attrs.buttons)) {
      buttons = (
        <div data-test-id="pageActions">
          {vnode.attrs.buttons}
        </div>
      );
    }

    return (<header class={style.pageHeader}>
      <div class={style.pageTitle}>
        {this.maybeSection(vnode)}
        <h1 class={style.title} data-test-id="title">{vnode.attrs.title}</h1>
        {helpText}
        <HeaderKeyValuePair data={vnode.attrs.keyValuePair}/>
      </div>
      {buttons}
    </header>);
  }

  private maybeSection(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.sectionName) {
      return (
        <h1 class={style.sectionName} data-test-id="section-name">{vnode.attrs.sectionName}</h1>
      );
    }
  }
}

class HelpTextModal extends Modal {
  private readonly msg: m.Children;

  constructor(msg: m.Children) {
    super();
    this.msg = msg;
  }

  body(): m.Children {
    return <div data-test-id="help-text-message">{this.msg}</div>;
  }

  title(): string {
    return "Show Help Text";
  }

}
