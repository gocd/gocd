/*
 * Copyright 2023 Thoughtworks, Inc.
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
//= require "lib/jquery-1.7.2.js"
//= require "lib/jquery-pinOnScroll.js"
//= require "lib/jquery_no_conflict.js"

// A number of things depend on this including ModalBox. Also causes some issues with hacks it applies to the browser
// toJSON that need to be worked around with Json.parse(Json.stringify(jsonObject)) in various places
//= require "lib/prototype-1.6.0.3.js"

// Used by Rails build/job detail page job history dropdown (data-toggle='dropdown' and .dropdown-menu)
//= require "lib/bootstrap-dropdown-2.3.2.js"

// used by Rails value_stream_map_renderer (look for $j(.*).draggable etc)
//= require "lib/jquery-ui-1.12.1.custom.min.js"

// Used by Rails stage details / stage history widget for showing config changes between stages. Relies on prototype+effects
//= require "lib/modalbox-1.6.1.js"

// Used by Rails job details page via timer_observer
//= require "lib/trimpath-template-1.0.38.js"

//= require "lib/lodash.js"

// Used by Rails job details and console log for formatting timestamps, along with Rails value stream map
//= require "lib/moment-2.29.4.js"
//= require "lib/moment-duration-format-2.3.2.js"
//= require "lib/humanize-for-gocd.js"

// Used by Rails job details console_log_socket.js
//= require "lib/pako_inflate-1.0.5.js"

// used by Rails job details console and websocket_wrapper.js
//= require "lib/often-0.3.2.js"
//= require "lib/component-emitter-1.2.1.js"

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

