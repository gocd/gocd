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

class MaterialConfigAPIModel
  def initialize(material_config_model)
    @attributes = Hash.new
    @attributes[:fingerprint] = material_config_model.getFingerprint()
    populate_map(@attributes, material_config_model.getAttributes(false))
  end

  def populate_map(ruby_map, java_map)
    java_map.each do |key, value|
      if (value.instance_of? java.util.HashMap)
        ruby_map[key] = Hash.new
        populate_map(ruby_map[key], value)
      else
        ruby_map[key] = value
      end
    end
  end

  def as_json(options = {})
    @attributes
  end
end