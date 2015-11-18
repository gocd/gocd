# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr #:nodoc:

  @@rspec_checked = false

  def self.rspec_present?
    unless @@rspec_checked
      begin
        require 'rspec'
      rescue LoadError
        # If Rspec is not present then that is ok
      end
      @@rspec_checked = true
    end

    # Need to check 'rspec.rb' for jruby-1.6.7.2 and earlier
    $LOADED_FEATURES.any?{|f| f == 'rspec.rb' || f =~ /[\\\/]rspec\.rb$/ }
  end

  def self.ensure_rspec(context)
    unless rspec_present?
      # Has the rspec dependency been loaded?
      message =
        "#{context} but RSpec has not been loaded.\n" +
          "\n" +
          "Buildr prior to version 1.4.22, included rspec as a dependency but as\n" +
          "of version 1.4.22, it is expected users manually add RSpec to their\n" +
          "Gemfile. The following lines should be added to restore the version\n" +
          "of rspec included in 1.4.22:\n" +
          "\n" +
          "gem 'rspec-expectations',   '= 2.14.3'\n" +
          "gem 'rspec-mocks',          '= 2.14.3'\n" +
          "gem 'rspec-core',           '= 2.14.5'\n" +
          "gem 'rspec',                '= 2.14.1'\n"
      fail message
    end
  end
end
