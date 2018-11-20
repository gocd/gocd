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
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import * as uuid from "uuid/v4";
import * as styles from "./forms.scss";

const classnames = bind(styles);

export interface HasProperty<T> {
  property: (newValue?: T) => T;
}

export interface LowlevelTextBindings<T> {
  oninput: (newValue: T) => any;
  value?: T;
}

export interface LowlevelCheckboxBindings {
  onclick: (newValue: boolean) => any;
  selected: boolean;
}

export interface LabelAttrs<T> {
  label: string;
  errorText?: string;
  helpText?: string;
  disabled?: boolean;
  required?: boolean;
}

type FormFieldAttrs<T> = LabelAttrs<T> & (HasProperty<T> | LowlevelTextBindings<T> | LowlevelCheckboxBindings);

function isHasPropertyInterface<T>(x: any): x is HasProperty<T> {
  return "property" in x;
}

abstract class FormField<T> extends MithrilViewComponent<FormFieldAttrs<T>> {
  protected readonly id: string         = `input-${uuid()}`;
  protected readonly helpTextId: string = `${this.id}-help-text`;
  protected readonly errorId: string    = `${this.id}-error-text`;

  view(vnode: m.Vnode<FormFieldAttrs<T>>) {
    const maybeRequired = this.isRequiredField(vnode) ? <span class={styles.formLabelRequired}>*</span> : undefined;
    return (
      <div className={classnames(styles.formGroup, {[styles.formHasError]: this.hasErrorText(vnode)})}>
        <label for={this.id} className={styles.formLabel}>{vnode.attrs.label}{maybeRequired}:</label>
        {this.renderInputField(vnode)}
        {this.errorSpan(vnode)}
        {this.getHelpSpan(vnode)}
      </div>
    );
  }

  abstract renderInputField(vnode: m.Vnode<FormFieldAttrs<T>>): m.Children | null | void;

  protected defaultAttributes(vnode: m.Vnode<FormFieldAttrs<T>>): { [key: string]: any } {
    const required = this.isRequiredField(vnode);

    const newVar: { [key: string]: string | boolean } = {
      "aria-label": vnode.attrs.label,
      "readonly": !!vnode.attrs.disabled,
      "required": !!required
    };

    if (this.hasHelpText(vnode)) {
      newVar["aria-describedby"] = this.helpTextId;
    }

    if (this.hasErrorText(vnode)) {
      newVar["aria-errormessage"] = this.errorId;
    }

    if (required) {
      newVar["aria-required"] = true;
      newVar.required         = true;
    }

    return newVar;
  }

  protected bindingAttributes(vnode: m.Vnode<FormFieldAttrs<T>>,
                              eventName: string,
                              propertyAttribute: string) {
    const valueAndOnChangeBindings: { [key: string]: any } = {};

    if (isHasPropertyInterface(vnode.attrs)) {
      valueAndOnChangeBindings[eventName]         = (evt: any) => (vnode.attrs as HasProperty<T>).property(evt.currentTarget.value);
      valueAndOnChangeBindings[propertyAttribute] = (vnode.attrs as HasProperty<T>).property();
    } else {
      valueAndOnChangeBindings[eventName]         = m.withAttr(propertyAttribute, (vnode.attrs as LowlevelTextBindings<T>).oninput);
      valueAndOnChangeBindings[propertyAttribute] = (vnode.attrs as LowlevelTextBindings<T>).value;
    }

    return valueAndOnChangeBindings;
  }

  protected getHelpSpan(vnode: m.Vnode<FormFieldAttrs<T>>) {
    if (this.hasHelpText(vnode)) {
      return (<span id={this.helpTextId} className={classnames(styles.formHelp)}>{vnode.attrs.helpText}</span>);
    }
  }

  protected hasHelpText(vnode: m.Vnode<FormFieldAttrs<T>>) {
    return !_.isEmpty(vnode.attrs.helpText);
  }

  protected hasErrorText(vnode: m.Vnode<FormFieldAttrs<T>>) {
    return !_.isEmpty(vnode.attrs.errorText);
  }

  private isRequiredField(vnode: m.Vnode<FormFieldAttrs<T>>) {
    return vnode.attrs.required;
  }

  private errorSpan(vnode: m.Vnode<FormFieldAttrs<T>>) {
    if (this.hasErrorText(vnode)) {
      return (
        <span className={styles.formErrorText} id={this.errorId}>{vnode.attrs.errorText}</span>
      );
    }
  }
}

export class TextField extends FormField<string> {
  renderInputField(vnode: m.Vnode<FormFieldAttrs<string>>) {

    return (
      <input type="text"
             {...this.defaultAttributes(vnode)}
             {...this.bindingAttributes(vnode, "oninput", "value")}
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
        className={classnames(styles.formControl, styles.textArea)}
        id={this.id}>{value}</textarea>
    );
  }
}

export class CheckboxField extends FormField<boolean> {
  renderInputField(vnode: m.Vnode<FormFieldAttrs<boolean>>): m.Children | void | null {
    return (
      <input type="checkbox"
             {...this.defaultAttributes(vnode)}
             {...this.bindingAttributes(vnode, "onchange", "checked")}
             className={classnames(styles.formControl)}
             id={this.id}/>
    );
  }
}

export class SelectField extends FormField<string> {
  renderInputField(vnode: m.Vnode<FormFieldAttrs<string>>): m.Children | void | null {
    return (
      <select
        {...this.defaultAttributes(vnode)}
        {...this.bindingAttributes(vnode, "onchange", "value")}>
        {vnode.children}
      </select>
    );
  }
}

export interface Option {
  id: string;
  text: string;
}

export interface SelectFieldAttrs {
  items: Array<Option | string>;
  selected?: string;
}

export class SelectFieldOptions extends MithrilViewComponent<SelectFieldAttrs> {
  view(vnode: m.Vnode<SelectFieldAttrs>): m.Children | void | null {
    return _.map(vnode.attrs.items, (optionOrString: Option | string) => {
      let id: string;
      let text: string;
      if (typeof optionOrString === "string") {
        id   = optionOrString as string;
        text = optionOrString as string;
      } else {
        id   = (optionOrString as Option).id;
        text = (optionOrString as Option).text;
      }

      return <option key={id}
                     value={id}
                     selected={vnode.attrs.selected === id}>{text}</option>;
    });
  }
}
