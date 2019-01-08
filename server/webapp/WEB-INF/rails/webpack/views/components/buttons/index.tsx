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

import {bind} from "classnames/bind";
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as styles from "./index.scss";

const classnames = bind(styles);

export enum ButtonIcon {
  ADD,
  DOC,
  FILTER,
}

export interface OnClickHandler {
  onclick?: (e: MouseEvent) => void;
}

export interface Attrs extends OnClickHandler {
  icon?: ButtonIcon;
  small?: boolean;
  disabled?: boolean;
  dropdown?: boolean;
}

class Button extends MithrilViewComponent<Attrs> {
  private readonly type: string;

  protected constructor(type: string) {
    super();
    this.type = type;
  }

  view(vnode: m.Vnode<Attrs>) {
    const isSmall    = vnode.attrs.small;
    const isDropdown = vnode.attrs.dropdown;

    return (
      <button {...vnode.attrs}
              onclick={vnode.attrs.onclick}
              className={classnames(
                styles.button,
                {[styles.btnSmall]: isSmall, [styles.btnDropdown]: isDropdown},
                Button.iconClass(vnode.attrs.icon),
                this.type
              )}>
        {vnode.children}
      </button>
    );
  }

  private static iconClass(icon?: ButtonIcon) {
    switch (icon) {
      case ButtonIcon.ADD:
        return styles.iconAdd;
      case ButtonIcon.DOC:
        return styles.iconDoc;
      case ButtonIcon.FILTER:
        return styles.iconFilter;
    }
    return "";
  }
}

export class Danger extends Button {
  constructor() {
    super(styles.btnDanger);
  }
}

export class Primary extends Button {
  constructor() {
    super(styles.btnPrimary);
  }
}

export class Secondary extends Button {
  constructor() {
    super(styles.btnSecondary);
  }
}

export class Reset extends Button {
  constructor() {
    super(styles.btnReset);
  }
}

export class Cancel extends Button {
  constructor() {
    super(styles.btnCancel);
  }
}

export class Link extends Button {
  constructor() {
    super(styles.btnLink);
  }
}

export class Default extends Button {
  constructor() {
    super(styles.btnDefault);
  }
}

export class ButtonGroup extends MithrilViewComponent<{}> {
  view(vnode: m.Vnode) {
    return (
      <div className={styles.buttonGroup} aria-label="actions">
        {vnode.children}
      </div>
    );
  }
}

export interface DropdownAttrs {
  show: Stream<boolean>;
}

export abstract class Dropdown<V = {}> extends MithrilComponent<DropdownAttrs & V> {
  oninit(vnode: m.Vnode<DropdownAttrs & V>) {
    document.body.addEventListener("click", this.closeDropdown.bind(this, vnode));
  }

  onremove(vnode: m.VnodeDOM<DropdownAttrs & V>) {
    document.body.removeEventListener("click", this.closeDropdown.bind(this, vnode));
  }

  toggleDropdown(vnode: m.Vnode<DropdownAttrs & V>, e: MouseEvent) {
    vnode.attrs.show(!vnode.attrs.show());
  }

  stopPropogation(e: Event) {
    e.stopPropagation();

    // redraw specifically and intentioally set to false here.
    // The reason is that, after any event, mithril's auto-redraw system redraws the component. This resets the changes in any input fields that user might have done on view.
    // To avoid that, redraw needs to set as false.
    // refer: https://mithril.js.org/autoredraw.html
    //@ts-ignore
    e.redraw = false;
  }

  view(vnode: m.Vnode<DropdownAttrs & V>) {
    return (
      <div className={classnames(this.classNames(), styles.btnDropdownContainer)}
           onclick={this.stopPropogation}>
        {this.doRenderButton(vnode)}
        {this.doRenderDropdownContent(vnode)}
      </div>
    );
  }

  protected classNames(): string | undefined {
    return undefined;
  }

  protected closeDropdown(vnode: m.Vnode<DropdownAttrs & V>) {
    vnode.attrs.show(false);
    m.redraw();
  }

  protected abstract doRenderButton(vnode: m.Vnode<DropdownAttrs & V>): m.Children;

  protected abstract doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & V>): m.Children;
}
