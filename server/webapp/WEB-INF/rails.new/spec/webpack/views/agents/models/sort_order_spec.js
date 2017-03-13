/*
 * Copyright 2017 ThoughtWorks, Inc.
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

describe("SortOrder", () => {

  const _         = require('lodash');
  const m         = require('mithril');
  const SortOrder = require('views/agents/models/sort_order');

  it("should default to correct values", () => {
    expect(new SortOrder().sortBy()).toEqual('agentState');
    expect(new SortOrder().orderBy()).toEqual('asc');
  });

  describe('toggleSortingOrder', () => {
    it("should toggle sort order on existing column", () => {
      const sortOrder     = new SortOrder();
      sortOrder.perform = _.noop;

      sortOrder.toggleSortingOrder(sortOrder.sortBy());
      expect(sortOrder.sortBy()).toEqual(sortOrder.sortBy());
      expect(sortOrder.orderBy()).toEqual('desc');

      sortOrder.toggleSortingOrder(sortOrder.sortBy());
      expect(sortOrder.sortBy()).toEqual(sortOrder.sortBy());
      expect(sortOrder.orderBy()).toEqual('asc');
    });

    it("should reset sort order (to asc) when column changes", () => {
      const sortOrder     = new SortOrder();
      sortOrder.perform = _.noop;

      sortOrder.toggleSortingOrder('bar');
      expect(sortOrder.sortBy()).toEqual('bar');
      expect(sortOrder.orderBy()).toEqual('asc');
    });
  });

  describe("isSortedOn", () => {
    it("should return true if current sort column is the same as the one passed in", () => {
      const sortOrder = new SortOrder();

      expect(sortOrder.isSortedOn(sortOrder.sortBy())).toBe(true);
      expect(sortOrder.isSortedOn('blah')).toBe(false);
    });
  });

  describe("initialize", () => {
    it("should initialize sortBy and orderBy values based on the routing params", () => {
      const sortOrder     = new SortOrder();
      sortOrder.perform = _.noop;

      spyOn(m.route, "param").and.returnValues('resources', 'desc');

      sortOrder.initialize();

      expect(sortOrder.sortBy()).toEqual('resources');
      expect(sortOrder.orderBy()).toEqual('desc');
    });

    it("should initialize sortBy and orderBy values to default when routing params are not present", () => {
      const sortOrder     = new SortOrder();
      sortOrder.perform = _.noop;

      spyOn(m.route, "param").and.returnValues(undefined, undefined);

      sortOrder.initialize();

      expect(sortOrder.sortBy()).toEqual('agentState');
      expect(sortOrder.orderBy()).toEqual('asc');
    });

    it("should perform routing on initialize", () => {
      const sortOrder = new SortOrder();

      const performSpy = spyOn(sortOrder, "perform");

      sortOrder.initialize();

      expect(performSpy).toHaveBeenCalled();
    });
  });
});
