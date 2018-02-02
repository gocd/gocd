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

  describe('initialize', () => {
    let pipelineNames, dashboardVM;
    beforeEach(() => {
      pipelineNames = ['up42', 'up43'];
      dashboardVM   = new DashboardVM();

    });

    it('should initialize VM with pipelines', () => {
      dashboardVM.initialize(pipelineNames);
      expect(dashboardVM.size()).toEqual(2);
    });

    it('should not initialize already initialized pipelines', () => {
      dashboardVM.initialize(pipelineNames);
      expect(dashboardVM.size()).toEqual(2);

      expect(dashboardVM.contains('up42')).toEqual(true);
      expect(dashboardVM.contains('up43')).toEqual(true);

      dashboardVM.initialize(pipelineNames);
      expect(dashboardVM.size()).toEqual(2);

      expect(dashboardVM.contains('up42')).toEqual(true);
      expect(dashboardVM.contains('up43')).toEqual(true);

      dashboardVM.initialize(['up42', 'up43', 'up44']);
      expect(dashboardVM.size()).toEqual(3);

      expect(dashboardVM.contains('up42')).toEqual(true);
      expect(dashboardVM.contains('up43')).toEqual(true);
      expect(dashboardVM.contains('up44')).toEqual(true);
    });

    it('should remove the unknown pipelines from the VM', () => {
      dashboardVM.initialize(pipelineNames);
      expect(dashboardVM.size()).toEqual(2);

      expect(dashboardVM.contains('up42')).toEqual(true);
      expect(dashboardVM.contains('up43')).toEqual(true);
      expect(dashboardVM.contains('up44')).toEqual(false);

      dashboardVM.initialize(['up43', 'up44']);
      expect(dashboardVM.size()).toEqual(2);

      expect(dashboardVM.contains('up42')).toEqual(false);
      expect(dashboardVM.contains('up43')).toEqual(true);
      expect(dashboardVM.contains('up44')).toEqual(true);
    });

  });

  describe("Dropdown", () => {
    let pipelineNames, dashboardVM;
    beforeEach(() => {
      pipelineNames = ['up42', 'up43'];
      dashboardVM   = new DashboardVM();
      dashboardVM.initialize(pipelineNames);
    });

    it('should initialize dropdown states for all the pipelines', () => {
      expect(dashboardVM.size()).toEqual(2);
    });

    it('should initialize dropdown states as close initially', () => {
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(false);
      expect(dashboardVM.dropdown.isDropDownOpen('up43')).toEqual(false);
    });

    it('should toggle dropdown state', () => {
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(false);
      dashboardVM.dropdown.toggle('up42');
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(true);
    });

    it('should close all other dropdowns incase of a dropdown is toggled', () => {
      dashboardVM.dropdown.toggle('up42');

      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(true);
      expect(dashboardVM.dropdown.isDropDownOpen('up43')).toEqual(false);
      dashboardVM.dropdown.toggle('up43');
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(false);
      expect(dashboardVM.dropdown.isDropDownOpen('up43')).toEqual(true);
    });

    it('should hide all dropdowns', () => {
      dashboardVM.dropdown.toggle('up42');
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(true);
      expect(dashboardVM.dropdown.isDropDownOpen('up43')).toEqual(false);
      dashboardVM.dropdown.hideAll();
      expect(dashboardVM.dropdown.isDropDownOpen('up42')).toEqual(false);
      expect(dashboardVM.dropdown.isDropDownOpen('up43')).toEqual(false);
    });
  });

  describe('Operation Messages', () => {
    let pipelineNames, dashboardVM;
    beforeEach(() => {
      pipelineNames = ['up42', 'up43'];
      dashboardVM   = new DashboardVM();
      dashboardVM.initialize(pipelineNames);
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('should initialize pipeline operation messages states for all the pipelines', () => {
      expect(dashboardVM.size()).toEqual(2);
    });

    it('should initialize pipeline operation messages states as empty initially', () => {
      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(undefined);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual(undefined);
    });

    it('should set pipeline operation success messages', () => {
      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(undefined);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual(undefined);

      const message = 'message';
      dashboardVM.operationMessages.success('up42', message);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(message);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual('success');
    });


    it('should set pipeline operation failure messages', () => {
      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(undefined);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual(undefined);

      const message = 'message';
      dashboardVM.operationMessages.failure('up42', message);

      expect(dashboardVM.operationMessages.messageFor('up42')).toEqual(message);
      expect(dashboardVM.operationMessages.messageTypeFor('up42')).toEqual('error');
    });

    it('should clear message after timeout interval', () => {
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

  describe('Search Text', () => {
    const dashboardVM = new DashboardVM();
    dashboardVM.initialize([]);

    it('should initialize search text field', () => {
      expect(dashboardVM.searchText()).toEqual('');
    });

    it('should create search text as a stream', () => {
      expect(dashboardVM.searchText()).toEqual('');
      const searchText = 'text for search';
      dashboardVM.searchText(searchText);

      expect(dashboardVM.searchText()).toEqual(searchText);
    });
  });
});
