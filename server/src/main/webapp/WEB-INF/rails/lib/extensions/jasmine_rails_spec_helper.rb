#
# Copyright 2021 ThoughtWorks, Inc.
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

if defined?(Jasmine)
  require 'jasmine/runners/selenium'
  module Jasmine
    module Runners
      class Selenium
        def run_details
          # Workaround for serialization bugs with FF/geckodriver
          # https://github.com/mozilla/geckodriver/issues/914
          # https://github.com/mozilla/geckodriver/issues/792

          # prototype patches `toJSON` methods on most JS types (arrays, strings, dates etc...)
          # this causes webdriver to not be able to read the return value (because the value is JSONized for marshalling)
          # we therefore, un-monkey-patch and serialize/deserialize to work around this :-/
          # but since we cannot really extract most data, we just simply blow up if there's an error
          run_details = driver.execute_script %Q{
              if (!jsApiReporter) {
                return;
              }

              return JSON.parse(JSON.stringify(jsApiReporter.runDetails))
          }

          if run_details['overallStatus'] === 'passed'
            exit(0)
          else
            $stderr.puts
            $stderr.puts("Some tests failed to execute! Please run in the browser and check the comment in #{__FILE__} for details!")
            exit 1
          end
        end
      end
    end
  end
end
