/*
 * Copyright 2024 Thoughtworks, Inc.
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
// This is a manifest file that'll be compiled into application.js, which will include all the files
// listed below.
//
// Any JavaScript/Coffee file within this directory, lib/assets/javascripts, vendor/assets/javascripts,
// or vendor/assets/javascripts of plugins, if any, can be referenced here using a relative path.
//
// It's not advisable to add code directly here, but if you do, it'll appear at the bottom of the
// compiled file.
//
// Read Sprockets README (https://github.com/rails/sprockets#directives) for details about supported directives

// Many things depend on jquery (incl direct code, jquery-ui and bootstrap)
//= require "lib/jquery-3.7.1.js"
//= require "lib/jquery-pinOnScroll.js"

// Used by Rails build/job detail page job history dropdown (data-toggle='dropdown' and .dropdown-menu)
//= require "lib/bootstrap-dropdown-2.3.2.js"

// used by Rails value_stream_map_renderer (look for $(.*).draggable etc)
//= require "lib/jquery-ui-1.14.0.js"

// Used by Rails job details page via timer_observer
//= require "lib/trimpath-template-1.0.38.js"

//= require "lib/lodash-4.17.21.js"

// Used by Rails job details and console log for formatting timestamps, along with Rails value stream map
//= require "lib/moment-2.30.1.js"
//= require "lib/moment-duration-format-2.3.2.js"
//= require "lib/humanize-for-gocd.js"

// Used by Rails job details console_log_socket.js
//= require "lib/pako_inflate-1.0.11.js"

// used by Rails job details console and websocket_wrapper.js
//= require "lib/often-0.3.2.js"
//= require "lib/component-emitter-1.3.0.js"

// GoCD's own shared functions
//= require "plugin-endpoint.js"
//= require "gocd-link-support.js"
//= require "plugin-endpoint-request-handler.js"

// Used within job details console log formatting
//= require "ansi_up.js"
//= require "crel.js"

//= require "json_to_css.js"
//= require "util.js"
//= require "micro_content_popup.js"
//= require "console_log_tailing.js"
//= require "js-routes"
//= require_directory .

