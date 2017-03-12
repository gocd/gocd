/*
 * Copyright 2015 ThoughtWorks, Inc.
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
describe("Stage Approval Model", () => {

  const s        = require("string-plus");
  const Approval = require("models/pipeline_configs/approval");

  let approval;
  beforeEach(() => {
    approval = new Approval({
      type:          'manual',
      authorization: new Approval.AuthConfig({roles: ['Administrators'], users: ['bob']})
    });
  });

  it("should initialize with type", () => {
    expect(approval.type()).toBe('manual');
  });

  it("should initialize with authorization", () => {
    expect(approval.authorization().roles()).toEqual(['Administrators']);
    expect(approval.authorization().users()).toEqual(['bob']);
  });

  describe("Serialization from/to JSON", () => {
    it("should de-serialize from json", () => {
      approval = Approval.fromJSON(sampleJSON());

      expect(approval.type()).toBe('manual');
      expect(approval.authorization().roles()).toEqual(['Administrators']);
      expect(approval.authorization().users()).toEqual(['bob']);
    });

    it("should serialize to json", () => {
      expect(JSON.parse(JSON.stringify(approval, s.snakeCaser))).toEqual(sampleJSON());
    });

    it("should serialize a comma separated user and role string to array", () => {
      approval.authorization().roles('Admins,Deployers,');
      approval.authorization().users('bob,alice,');

      expect(JSON.parse(JSON.stringify(approval, s.snakeCaser))['authorization']).toEqual({
        roles: ['Admins', 'Deployers'],
        users: ['bob', 'alice']
      });
    });

    function sampleJSON() {
      return {
        type:          'manual',
        authorization: {
          users: ['bob'],
          roles: ['Administrators']
        }
      };
    }

  });

});
