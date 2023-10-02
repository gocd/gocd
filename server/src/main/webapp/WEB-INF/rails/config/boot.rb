#
# Copyright 2023 Thoughtworks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ENV["BUNDLE_GEMFILE"] ||= File.expand_path("../Gemfile", __dir__)

# When running in production (NOT just building FOR production), force bundler to ignore groups including the assets pipeline stuff
if ENV["RAILS_ENV"] == "production" and not ENV.has_key?("RAILS_GROUPS")
  require "bundler"
  Bundler.setup(:default)
else
  require "bundler/setup" # Set up gems listed in the Gemfile.
end
