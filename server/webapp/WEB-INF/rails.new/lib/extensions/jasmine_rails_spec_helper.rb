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
unless Rails.env.production?
  require 'jasmine-core/version'
  require 'jasmine-core'
  require 'jasmine'

  module Jasmine
    raise "This jasmine patch is only tested with #{Jasmine::Core::VERSION} of jasmine-core" unless Jasmine::Core::VERSION == '2.8.0'

    def self.runner_template
      File.read(Rails.root.join("spec/javascripts/run.html.erb"))
    end

    # The jasmine gem does 2 things:
    #  - provide jasmine JS integration with rails (sprockets, rails etc...)
    #  - provide a means of packaging the jasmine JS framework (jasmine js + jsamine css)
    #
    # Our tests strictly requires jasmine 2.4.1 (which does not work with rails 5)
    # So we patch a newer jasmine gem, to render old jasmine JS framework
    module Core
      class << self
        def path
          File.join(File.dirname(__FILE__), "jasmine")
        end
      end
    end
  end
end
