#
# Copyright 2024 Thoughtworks, Inc.
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

# Be sure to restart your server when you modify this file.

# Version of your assets, change this if you want to expire all your assets.
Rails.application.config.assets.version = "1.0"

# Add additional assets to the asset load path.
Rails.application.config.assets.paths << Rails.root.join("node_modules")
Rails.application.config.assets.paths << Rails.root.join("webpack", "rails-shared")

# Precompile additional assets.
# application.js, application.css, and all non-JS/CSS in the app/assets
# folder are already added.
# Rails.application.config.assets.precompile += %w( admin.js admin.css )

# Monkey patch Open3.popen_run to workaround Java 21 issue with JRuby when running sass-embedded as documented at
# - https://github.com/sass-contrib/sass-embedded-host-ruby/issues/208
# - https://github.com/jruby/jruby/issues/8235
# - https://github.com/jruby/jruby/issues/8069
# - https://bugs.openjdk.org/browse/JDK-8329604
# Remove for Java 21.0.4 onwards where a fix for Java 23 is backported.
if ENV["RAILS_ENV"] != "production" or ENV.has_key?("RAILS_GROUPS")
  require 'fcntl'

  module Open3
    _popen_run = instance_method(:popen_run)
    define_method(:popen_run) do |cmd, opts, child_io, parent_io, &block|
      child_io.each do |io|
        flags = io.fcntl(Fcntl::F_GETFL)
        io.fcntl(Fcntl::F_SETFL, flags | (Fcntl::O_NONBLOCK))
        io.fcntl(Fcntl::F_SETFL, flags & (~Fcntl::O_NONBLOCK))
      end
      _popen_run.bind(self).call(cmd, opts, child_io, parent_io, &block)
    end
    module_function :popen_run
    class << self
      private :popen_run
    end
  end
end
