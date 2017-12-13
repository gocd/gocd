##########################################################################
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
##########################################################################

module ApiV2
  module Dashboard
    class MaterialRevisionsRepresenter < ApiV2::BaseRepresenter
      alias_method :material_revision, :represented

      property :getMaterialType, as: :material_type
      property :getMaterialName, as: :material_name
      property :isChanged, as: :changed

      collection :modifications, exec_context: :decorator,
                 decorator: lambda {|material, *|
                   material[:material].getTypeForDisplay() == 'Pipeline' ? PipelineDependencyModificationRepresenter : ModificationRepresenter
                 }

      def modifications
        material_revision.getModifications().inject([]) {|r, modification| r << {material: material_revision.getMaterial(), modification: modification, latest_revision: material_revision.getRevision()}}
      end
    end
  end
end
