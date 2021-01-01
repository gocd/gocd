/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import * as simulateEvent from "simulate-event";
import * as Buttons from "views/components/buttons";
import {Step, Wizard} from "views/components/wizard/index";
import styles from "../index.scss";

describe("WizardSpec", () => {
  let wizard: Wizard;
  beforeEach(() => {
    wizard = new Wizard()
      .addStep(new SampleStep("Step 1", "This is step one"))
      .addStep(new SampleStep("Step 2", "This is step two"))
      .addStep(new SampleStep("Step 3", "This is step three"));
  });

  afterEach(() => {
    wizard.close();
    m.redraw.sync();

    expect(findByClass(styles.wizard)).not.toBeInDOM();
  });

  it("should display wizard", () => {
    expect(findByClass(styles.wizard)).not.toBeInDOM();

    wizard.render();
    m.redraw.sync();

    expect(findByClass(styles.wizard)).not.toBeNull();
  });

  it("should select the given step", () => {
    wizard
      .defaultStepIndex(2)
      .render();
    m.redraw.sync();

    expect(findIn(styles.wizardHeader, styles.stepHeader)[1]).toHaveClass(styles.selected);
    expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step two");
  });

  describe("Wizard steps", () => {
    it("should list all the wizard steps in the header", () => {
      wizard.render();
      m.redraw.sync();

      expect(findByClass(styles.wizard)).not.toBeNull();
      expect(findIn(styles.wizardHeader, styles.stepHeader)[0]).toHaveText("Step 1");
      expect(findIn(styles.wizardHeader, styles.stepHeader)[1]).toHaveText("Step 2");
    });

    it("should show content of the first step by default", () => {
      wizard.render();
      m.redraw.sync();

      expect(findByClass(styles.wizard)).not.toBeNull();
      expect(findIn(styles.wizardBody, styles.stepBody).length).toBe(1);
      expect(findIn(styles.wizardBody, styles.stepBody)[0]).toHaveText("This is step one");
      expect(findIn(styles.wizardBody, styles.stepBody)[0]).toBeVisible();
    });

    it("should switch to step on click of the step name", () => {
      wizard.render();
      m.redraw.sync();

      expect(findByClass(styles.wizard)).not.toBeNull();
      expect(findIn(styles.wizardBody, styles.stepBody).length).toBe(1);
      expect(findIn(styles.wizardBody, styles.stepBody)[0]).toHaveText("This is step one");

      simulateEvent.simulate(findIn(styles.wizardHeader, styles.stepHeader)[1], "click");
      m.redraw.sync();

      expect(findIn(styles.wizardBody, styles.stepBody)[0]).toHaveText("This is step two");
    });

    it("should not switch to step on click of step name when turned off", () => {
      wizard.allowHeaderClick = false;
      wizard.render();
      m.redraw.sync();

      expect(findByClass(styles.wizard)).not.toBeNull();
      expect(findIn(styles.wizardBody, styles.stepBody).length).toBe(1);
      expect(findIn(styles.wizardBody, styles.stepBody)[0]).toHaveText("This is step one");

      simulateEvent.simulate(findIn(styles.wizardHeader, styles.stepHeader)[1], "click");
      m.redraw.sync();

      expect(findIn(styles.wizardBody, styles.stepBody)[0]).toHaveText("This is step one");
    });
  });

  describe("Cancel button", () => {
    it("should show cancel button", () => {
      wizard.render();
      m.redraw.sync();

      expect(findByDataTestId("cancel")).toBeInDOM();
      expect(findByDataTestId("cancel")).toHaveText("Cancel");
    });

    it("should close wizard on click", () => {
      wizard.render();
      m.redraw.sync();

      expect(findByClass(styles.wizard)).not.toBeNull();
      expect(findByDataTestId("cancel")).toBeInDOM();

      simulateEvent.simulate(findByDataTestId("cancel")[0], "click");
      m.redraw.sync();

      expect(findByClass(styles.wizard)).not.toBeInDOM();
    });
  });

  describe("Previous button", () => {
    it("should show previous button", () => {
      wizard.render();
      m.redraw.sync();

      expect(findByDataTestId("previous")).toBeInDOM();
      expect(findByDataTestId("previous")).toHaveText("Previous");
    });

    it("should go to previous step when clicked", () => {
      wizard.defaultStepIndex(2).render();
      m.redraw.sync();

      expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step two");

      simulateEvent.simulate(findByDataTestId("previous")[0], "click");
      m.redraw.sync();

      expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step one");
    });

    it("should disable previous button on first step", () => {
      wizard.defaultStepIndex(1).render();
      m.redraw.sync();

      expect(findByDataTestId("previous")).toBeDisabled();
    });
  });

  describe("Next Button", () => {
    it("should show next button", () => {
      wizard.render();
      m.redraw.sync();

      expect(findByDataTestId("next")).toBeInDOM();
      expect(findByDataTestId("next")).toHaveText("Next");
    });

    it("should go to next step when clicked", () => {
      wizard.defaultStepIndex(1).render();
      m.redraw.sync();

      expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step one");

      simulateEvent.simulate(findByDataTestId("next")[0], "click");
      m.redraw.sync();

      expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step two");
    });

    it("should disable next button on last step", () => {
      wizard.defaultStepIndex(3).render();
      m.redraw.sync();

      expect(findByDataTestId("next")).toBeDisabled();
    });
  });

  describe("Custom Footer", () => {
    it("should render wizard with custom footer", () => {
      const customButton = <Buttons.Primary data-test-id="customButton">Custom Button</Buttons.Primary>;
      wizard.addStep(new SampleStep("last Step", "this is last step", customButton));

      wizard.defaultStepIndex(4).render();
      m.redraw.sync();

      expect(findByDataTestId("customButton")).toBeInDOM();
      expect(findByDataTestId("customButton")).toHaveText("Custom Button");
    });
  });

  function findByClass(className: string) {
    return document.getElementsByClassName(className)[0];
  }

  function findByDataTestId(id: string) {
    return document.querySelectorAll(`[data-test-id='${id}']`);
  }

  function findIn(parent: string, childClass: string) {
    return document.querySelectorAll(`.${parent} .${childClass}`);
  }
});

class SampleStep extends Step {
  private readonly name: string;
  private readonly content: m.Children;
  private readonly buttons: m.Children;

  constructor(name: string, body: m.Children, footer?: m.Children) {
    super();
    this.name    = name;
    this.content = body;
    this.buttons = footer;

  }

  body() {
    return this.content;
  }

  footer(wizard: Wizard): m.Children {
    return (<div>
      {super.footer(wizard)}
      {this.buttons}
    </div>);
  }

  header() {
    return this.name;
  }
}
