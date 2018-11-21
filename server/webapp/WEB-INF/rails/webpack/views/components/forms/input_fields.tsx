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
import {EncryptedValue} from "views/components/forms/encrypted_value";
import * as styles from "./forms.scss";

const classnames = bind(styles);

export interface HasProperty<T> {
  property: (newValue?: T) => T;
}

export interface LabelAttrs<T> {
  label: string;
  errorText?: string;
  helpText?: string;
  disabled?: boolean;
  required?: boolean;
}

type FormFieldAttrs<T> = LabelAttrs<T> & HasProperty<T>;

abstract class FormField<T> extends MithrilViewComponent<FormFieldAttrs<T>> {
  protected readonly id: string         = `input-${uuid()}`;
  protected readonly helpTextId: string = `${this.id}-help-text`;
  protected readonly errorId: string    = `${this.id}-error-text`;

  view(vnode: m.Vnode<FormFieldAttrs<T>>) {
    const maybeRequired = this.isRequiredField(vnode) ?
      <span className={styles.formLabelRequired}>*</span> : undefined;
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
      "required": !!required,
      "autocomplete": "off",
      "autocapitalize": "off",
      "autocorrect": "off",
      "spellcheck": false,
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

  // moved
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

  protected isRequiredField(vnode: m.Vnode<FormFieldAttrs<T>>) {
    return vnode.attrs.required;
  }

  protected errorSpan(vnode: m.Vnode<FormFieldAttrs<T>>) {
    if (this.hasErrorText(vnode)) {
      return (
        <span className={styles.formErrorText} id={this.errorId}>{vnode.attrs.errorText}</span>
      );
    }
  }

  protected bindingAttributes(vnode: m.Vnode<FormFieldAttrs<T>>,
                              eventName: string,
                              propertyAttribute: string): { [key: string]: any } {
    return {
      [eventName]: (evt: any) => (vnode.attrs as HasProperty<T>).property(evt.currentTarget.value),
      [propertyAttribute]: (vnode.attrs as HasProperty<T>).property()
    };
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

export class PasswordField extends FormField<EncryptedValue> {
  renderInputField(vnode: m.Vnode<FormFieldAttrs<EncryptedValue>>) {
    const input = <input type="password"
                         {...this.defaultAttributes(vnode)}
                         {...this.bindingAttributes(vnode, "oninput", "value")}
                         className={classnames(styles.formControl, styles.inline)}
                         id={this.id}/>;

    return [input, PasswordField.resetOrOverride(vnode)];
  }

  protected defaultAttributes(vnode: m.Vnode<FormFieldAttrs<EncryptedValue>>): { [p: string]: any } {
    const defaultAttributes = super.defaultAttributes(vnode);
    if (!vnode.attrs.property().isEditing()) {
      defaultAttributes.readonly = true;
    }
    return defaultAttributes;
  }

  protected bindingAttributes(vnode: m.Vnode<FormFieldAttrs<EncryptedValue>>,
                              eventName: string,
                              propertyAttribute: string) {

    if (vnode.attrs.property().isEditing()) {
      return {
        [eventName]: (evt: any) => vnode.attrs.property().value(evt.currentTarget.value),
        [propertyAttribute]: vnode.attrs.property().value()
      };
    } else {
      return {
        value: "************"
      };
    }
  }

  private static resetOrOverride(vnode: m.Vnode<FormFieldAttrs<EncryptedValue>>) {
    if (vnode.attrs.property().isEditing()) {
      return <a href="javascript:void(0)"
                className={classnames(styles.overrideEncryptedValue)}
                onclick={vnode.attrs.property().resetToOriginal.bind(vnode.attrs.property())}>Reset</a>;
    } else {
      return <a href="javascript:void(0)"
                className={classnames(styles.resetEncryptedValue)}
                onclick={vnode.attrs.property().edit.bind(vnode.attrs.property())}>Change</a>;
    }
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
