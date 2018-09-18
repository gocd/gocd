##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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

module MockRegistryModule
  class MockRegistry
    include com.thoughtworks.go.config.registry.ConfigElementRegistry
    def implementersOf(config_element_type)
      [com.thoughtworks.go.config.ExecTask.java_class,
       com.thoughtworks.go.config.AntTask.java_class,
       com.thoughtworks.go.config.NantTask.java_class,
       com.thoughtworks.go.config.RakeTask.java_class
      ]
    end
  end
end
