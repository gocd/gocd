/*
 * Copyright 2019 ThoughtWorks, Inc.
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
describe("Agents Route Handler", () => {

  const _            = require('lodash');
  const m            = require('mithril');
  const RouteHandler = require('views/agents/models/route_handler');

  it("should default to correct values", () => {
    expect(new RouteHandler().sortBy()).toEqual('agentState');
    expect(new RouteHandler().orderBy()).toEqual('asc');

    expect(new RouteHandler().searchText()).toEqual('');
  });

  describe('toggleSortingOrder', () => {
    it("should toggle sort order on existing column", () => {
      const routeHandler   = new RouteHandler();
      routeHandler.perform = _.noop;

      routeHandler.toggleSortingOrder(routeHandler.sortBy());
      expect(routeHandler.sortBy()).toEqual(routeHandler.sortBy());
      expect(routeHandler.orderBy()).toEqual('desc');

      routeHandler.toggleSortingOrder(routeHandler.sortBy());
      expect(routeHandler.sortBy()).toEqual(routeHandler.sortBy());
      expect(routeHandler.orderBy()).toEqual('asc');
    });

    it("should reset sort order (to asc) when column changes", () => {
      const routeHandler   = new RouteHandler();
      routeHandler.perform = _.noop;

      routeHandler.toggleSortingOrder('bar');
      expect(routeHandler.sortBy()).toEqual('bar');
      expect(routeHandler.orderBy()).toEqual('asc');
    });
  });

  describe("isSortedOn", () => {
    it("should return true if current sort column is the same as the one passed in", () => {
      const routeHandler = new RouteHandler();

      expect(routeHandler.isSortedOn(routeHandler.sortBy())).toBe(true);
      expect(routeHandler.isSortedOn('blah')).toBe(false);
    });
  });

  describe('searchText', () => {
    it('should return searchText value when no params are provided', () => {
      const routeHandler   = new RouteHandler();
      routeHandler.perform = _.noop;

      expect(routeHandler.searchText()).toEqual('');
    });

    it('should set searchText provided search value', () => {
      const routeHandler   = new RouteHandler();
      routeHandler.perform = _.noop;

      expect(routeHandler.searchText()).toEqual('');

      const searchedBy = 'filter';
      routeHandler.searchText(searchedBy);

      expect(routeHandler.searchText()).toEqual(searchedBy);
    });

    it('should perform routing when search Text is updated', () => {
      const routeHandler = new RouteHandler();
      const performSpy   = spyOn(routeHandler, "perform");

      expect(performSpy).not.toHaveBeenCalled();

      routeHandler.searchText('searchedBy');

      expect(performSpy).toHaveBeenCalled();
    });
  });

  describe("initialize", () => {
    it("should initialize sortBy, orderBy and searchText values based on the routing params", () => {
      const routeHandler   = new RouteHandler();
      routeHandler.perform = _.noop;

      spyOn(m.route, "param").and.returnValues('resources', 'desc', 'filterText');

      routeHandler.initialize();

      expect(routeHandler.sortBy()).toEqual('resources');
      expect(routeHandler.orderBy()).toEqual('desc');
      expect(routeHandler.searchText()).toEqual('filterText');
    });

    it("should initialize sortBy, orderBy and searchText values to default when routing params are not present", () => {
      const routeHandler   = new RouteHandler();
      routeHandler.perform = _.noop;

      spyOn(m.route, "param").and.returnValues(undefined, undefined, undefined);

      routeHandler.initialize();

      expect(routeHandler.sortBy()).toEqual('agentState');
      expect(routeHandler.orderBy()).toEqual('asc');
      expect(routeHandler.searchText()).toEqual('');
    });

    it("should perform routing on initialize", () => {
      const routeHandler = new RouteHandler();

      const performSpy = spyOn(routeHandler, "perform");

      routeHandler.initialize();

      expect(performSpy).toHaveBeenCalled();
    });
  });
});
