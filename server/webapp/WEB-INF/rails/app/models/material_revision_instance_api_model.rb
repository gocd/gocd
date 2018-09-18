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

class MaterialRevisionInstanceAPIModel
  attr_reader :material, :changed, :modifications

  def initialize(material_revision_instance_model)
    @material = MaterialInstanceAPIModel.new(material_revision_instance_model.getMaterial())
    @changed = material_revision_instance_model.isChanged()
    @modifications = material_revision_instance_model.getModifications().collect do |modification_instance_model|
      ModificationInstanceAPIModel.new(modification_instance_model)
    end
  end
end