/*
 * Copyright Thoughtworks, Inc.
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
import classNames from "classnames";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {HeaderKeyValuePair} from "views/components/header_panel/header_key_value_pair";
import {Help, TooltipSize} from "views/components/tooltip";
import style from "./index.scss";

const classnames = classNames.bind(style);

interface HelpTextAttrs {
  help?: m.Children;
}

export interface Attrs extends HelpTextAttrs {
  title: m.Children;
  sectionName?: m.Children;
  buttons?: m.Children;
  keyValuePair?: [m.Children, m.Children];
  bottomBorderColor?: 'passed' | 'failed' | 'building' | 'unknown';
}

export class HeaderPanel extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let buttons: m.Children = null;

    if (!_.isEmpty(vnode.attrs.buttons)) {
      buttons = (
        <div data-test-id="pageActions">
          {vnode.attrs.buttons}
        </div>
      );
    }

    const helpText = (vnode.attrs.sectionName)
      ? undefined
      : <HelpTextWidget help={vnode.attrs.help}/>;

    return (<header class={classnames(style.pageHeader, style[vnode.attrs.bottomBorderColor || 'unknown'])}>
      <div class={style.pageTitle}>
        {this.maybeSection(vnode)}
        {this.maybeTitle(vnode)}
        {helpText}
        {HeaderPanel.maybeKeyValuePairs(vnode)}
        {vnode.children}
      </div>
      {buttons}
    </header>);
  }

  private static maybeKeyValuePairs(vnode: m.Vnode<Attrs>) {
    return <HeaderKeyValuePair data={vnode.attrs.keyValuePair}/>;
  }

  private maybeSection(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.sectionName) {
      return <div class={style.sectionWrapper} data-test-id="section-wrapper">
        <h1 class={style.sectionName} data-test-id="section-name">{vnode.attrs.sectionName}</h1>
        <HelpTextWidget help={vnode.attrs.help}/>
      </div>;
    }
  }

  private maybeTitle(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.title) {
      return (<h1 className={style.title} data-test-id="title">{vnode.attrs.title}</h1>);
    }
  }
}

class HelpTextWidget extends MithrilViewComponent<HelpTextAttrs> {
  view(vnode: m.Vnode<HelpTextAttrs, this>): m.Children | void | null {
    if (!vnode.attrs.help) {
      return undefined;
    }
    return <div data-test-id="help-text-wrapper" className={style.helpTextWrapper}>
      <Help content={vnode.attrs.help} size={TooltipSize.medium}/>
    </div>;
  }
}
