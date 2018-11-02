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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import * as uuid from "uuid/v4";

const styles     = require("./../forms.scss");
const classnames = require("classnames/bind").bind(styles);

export interface HasProperty<T> {
  property: (newValue?: T) => T;
}

export interface Lowlevel<T> {
  onchange: (newValue: T) => any;
  value: T;
}

export interface LabelAttrs<T> {
  label: string;
  helpText?: string;
  disabled?: boolean;
}

type FormFieldAttrs<T> = LabelAttrs<T> & (HasProperty<T> | Lowlevel<T>);

function isHasPropertyInterface<T>(x: any): x is HasProperty<T> {
  return "property" in x;
}

abstract class FormField<T> extends MithrilViewComponent<FormFieldAttrs<T>> {
  protected readonly id: string = `item-${uuid()}`;

  view(vnode: m.Vnode<FormFieldAttrs<T>>) {
    return (
      <li className={classnames(styles.formGroup)}>
        <label htmlFor={this.id} className={classnames(styles.formLabel, "required")}>{vnode.attrs.label}:</label>
        {this.renderInputField(vnode)}
        <span className={classnames(styles.formHelp)}>{vnode.attrs.helpText}</span>
      </li>
    );
  }

  abstract renderInputField(vnode: m.Vnode<FormFieldAttrs<T>>): m.Children | null | void;

  protected defaultAttributes(vnode: m.Vnode<FormFieldAttrs<T>>) {
    const valueAndOnChangeBindings: { [key: string]: any } = {
      "aria-label": vnode.attrs.label,
      "readonly": vnode.attrs.disabled
    };

    if (isHasPropertyInterface(vnode.attrs)) {
      valueAndOnChangeBindings.onchange = (evt: any) => (vnode.attrs as HasProperty<T>).property(evt.currentTarget.value);
      valueAndOnChangeBindings.value    = (vnode.attrs as HasProperty<T>).property();
    } else {
      valueAndOnChangeBindings.onchange = (evt: any) => (vnode.attrs as Lowlevel<T>).onchange;
      valueAndOnChangeBindings.value    = (vnode.attrs as Lowlevel<T>).value;
    }
    return valueAndOnChangeBindings;
  }
}

export class TextField extends FormField<string> {
  renderInputField(vnode: m.Vnode<FormFieldAttrs<string>>) {

    return (
      <input type="text"
             {...this.defaultAttributes(vnode)}
             className={classnames(styles.formControl)}
             id={this.id}/>
    );
  }

}

export class TextAreaField extends FormField<string> {
  renderInputField(vnode: m.Vnode<FormFieldAttrs<string>>) {

    const defaultAttributes = this.defaultAttributes(vnode);

    const value = defaultAttributes.value;

    delete defaultAttributes.value;

    return (
      <textarea
        {...defaultAttributes}
        className={classnames(styles.formControl)}
        id={this.id}>{value}</textarea>
    );
  }

}

export class CheckboxField extends FormField<boolean> {
  renderInputField(vnode: m.Vnode<FormFieldAttrs<boolean>>): m.Children | void | null {
    return (
      <input type="checkbox"
             {...this.defaultAttributes(vnode)}
             className={classnames(styles.formControl)}
             id={this.id}/>
    );
  }
}
