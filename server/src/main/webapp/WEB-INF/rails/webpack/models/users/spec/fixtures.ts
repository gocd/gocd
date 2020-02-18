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

import {UsersJSON} from "models/users/users";

export function usersJSON(): UsersJSON {
  return {
    _embedded: {
      users: [
        {
          login_name: "root",
          is_admin: true,
          display_name: "root",
          enabled: false,
          email: undefined,
          email_me: false,
          checkin_aliases: [],
          roles: [{
            name: "gocd-admin",
            type: "gocd"
          }]
        },
        {
          login_name: "jez",
          is_admin: true,
          display_name: "jez",
          enabled: true,
          email: undefined,
          email_me: false,
          checkin_aliases: [],
          roles: [{
            name: "nobody",
            type: "gocd"
          }]
        },
        {
          login_name: "jigsaw",
          is_admin: true,
          display_name: "jigsaw",
          enabled: false,
          email: undefined,
          email_me: false,
          checkin_aliases: [],
          roles: []
        },
        {
          login_name: "admin",
          is_admin: true,
          display_name: "admin",
          enabled: true,
          email: "admin@example.com",
          email_me: false,
          checkin_aliases: [
            "alias1@gmail.com",
            "alias2@example.com"
          ],
          roles: [{
            name: "gocd-admin",
            type: "gocd"
          }]
        },
        {
          login_name: "cruise_admin",
          is_admin: true,
          display_name: "cruise_admin",
          enabled: true,
          email: undefined,
          email_me: false,
          checkin_aliases: [],
          roles: []
        },
        {
          login_name: "operate",
          is_admin: false,
          display_name: "operate",
          enabled: true,
          email: undefined,
          email_me: false,
          checkin_aliases: [],
          roles: []
        },
        {
          login_name: "view",
          is_admin: false,
          display_name: "view",
          enabled: true,
          email: undefined,
          email_me: false,
          checkin_aliases: [],
          roles: []
        }
      ]
    }
  };
}
