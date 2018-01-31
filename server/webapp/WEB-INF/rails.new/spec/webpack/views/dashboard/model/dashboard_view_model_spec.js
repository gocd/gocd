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

describe("Dashboard View Model", () => {
  const DashboardVM = require("views/dashboard/models/dashboard_view_model");

  describe("Dropdown", () => {
    const pipelineNames = ['up42', 'up43'];

    let dashboardVM;
    it('should initialize dropdown states for all the pipelines', () => {
      dashboardVM = new DashboardVM(pipelineNames);

      expect(dashboardVM.size()).toEqual(2);
    });

    it('should initialize dropdown states as close initially', () => {
      dashboardVM = new DashboardVM(pipelineNames);

      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(false);
      expect(dashboardVM.dropdown.isDropDownOpen('up43')).toEqual(false);
    });

    it('should toggle dropdown state', () => {
      dashboardVM = new DashboardVM(pipelineNames);

      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(false);
      dashboardVM.dropdown.toggle('up42');
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(true);
    });

    it('should close all other dropdowns incase of a dropdown is toggled', () => {
      dashboardVM = new DashboardVM(pipelineNames);
      dashboardVM.dropdown.toggle('up42');

      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(true);
      expect(dashboardVM.dropdown.isDropDownOpen('up43')).toEqual(false);
      dashboardVM.dropdown.toggle('up43');
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(false);
      expect(dashboardVM.dropdown.isDropDownOpen('up43')).toEqual(true);
    });

    it('should hide all dropdowns', () => {
      dashboardVM = new DashboardVM(pipelineNames);
      dashboardVM.dropdown.toggle('up42');
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(true);
      dashboardVM.dropdown.hideAll();
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(false);
    });
  });

  describe('Operation Messages', () => {
    const pipelineNames = ['up42', 'up43'];

    beforeEach(() => {
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    let dashboardVM;
    it('should initialize pipeline operation messages states for all the pipelines', () => {
      dashboardVM = new DashboardVM(pipelineNames);

      expect(dashboardVM.size()).toEqual(2);
    });

    it('should initialize pipeline operation messages states as empty initially', () => {
      dashboardVM = new DashboardVM(pipelineNames);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(undefined);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual(undefined);
    });

    it('should set pipeline operation success messages', () => {
      dashboardVM = new DashboardVM(pipelineNames);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(undefined);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual(undefined);

      const message = 'message';
      dashboardVM.operationMessages.success('up42', message);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(message);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual('success');
    });


    it('should set pipeline operation failure messages', () => {
      dashboardVM = new DashboardVM(pipelineNames);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(undefined);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual(undefined);

      const message = 'message';
      dashboardVM.operationMessages.failure('up42', message);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(message);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual('error');
    });

    it('should clear message after timeout interval', () => {
      dashboardVM = new DashboardVM(pipelineNames);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(undefined);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual(undefined);

      const message = 'message';
      dashboardVM.operationMessages.failure('up42', message);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(message);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual('error');

      jasmine.clock().tick(5001);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(undefined);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual(undefined);
    });
  });
});
