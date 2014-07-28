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

class MaterialInstanceAPIModel
  attr_reader :id, :fingerprint, :type, :description

  def initialize(material_instance_model)
    @id = material_instance_model.getId() if (material_instance_model.respond_to? :getId) && (material_instance_model.getId() != nil)
    @fingerprint = material_instance_model.getFingerprint() unless material_instance_model.getFingerprint() == nil
    @type = material_instance_model.getTypeForDisplay() unless material_instance_model.getTypeForDisplay() == nil
    @description = material_instance_model.getLongDescription() unless material_instance_model.getLongDescription() == nil
  end
end