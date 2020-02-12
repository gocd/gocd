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
import m from 'mithril';
import * as Buttons from "../../buttons";
import {Modal, Size} from "../index";
import styles from './index.scss';
import {Sample2} from "./sample2";

export class SampleModal extends Modal {

  secondModal: Modal;

  constructor(size: Size) {
    super(size);
    this.secondModal = new Sample2();
  }

  validate() {
    if ($(`.${styles.inputText}`).val() === 'hello') {
      this.close();
    } else {
      alert("Validation failed");
    }
  }

  title() {
    return "This is a sample modal";
  }

  body() {
    return <div>
      <p> This is the modal body </p>
      <p> Enter 'hello' in the textbox and click 'ok' </p>
      <input class={styles.inputText} type="text"/>
      <br/>
      <div>
        <Buttons.Secondary onclick={this.secondModal.render.bind(this.secondModal)}>Open another Modal</Buttons.Secondary>
      </div>
    </div>;
  }

  buttons() {
    return [
      <Buttons.Primary onclick={this.validate.bind(this)}>OK</Buttons.Primary>,
      <Buttons.Cancel onclick={this.close.bind(this)}>Cancel</Buttons.Cancel>
    ];
  }
}
