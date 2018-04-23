##########################GO-LICENSE-START################################
# Copyright 2017 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################
# This file is sourced by jruby-rack and is used to perform initialization of the jruby environment
# because jruby-rack does not respect GEM_HOME/GEM_PATH set in web.xml
if $servlet_context
  ENV['GEM_HOME']       = $servlet_context.getRealPath('/WEB-INF/rails.new/vendor/bundle/jruby/2.3.0')
  ENV['BUNDLE_GEMFILE'] ||= $servlet_context.getRealPath('/WEB-INF/rails.new/Gemfile')
else
  ENV['GEM_HOME']       = File.expand_path(File.join('..', '/rails.new/vendor/bundle/jruby/2.3.0'), __FILE__)
  ENV['BUNDLE_GEMFILE'] ||= File.expand_path(File.join('..', '/rails.new/Gemfile'), __FILE__)
end

ENV['RAILS_ENV']      ||= (ENV['RACK_ENV'] || 'production')

if ENV['RAILS_ENV'] == 'production'
  ENV['EXECJS_RUNTIME'] = 'Disabled'
  ENV['BUNDLE_WITHOUT'] = 'development:test:assets'
end