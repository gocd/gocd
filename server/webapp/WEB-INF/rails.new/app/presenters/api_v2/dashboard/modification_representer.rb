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
    class ModificationRepresenter < ApiV2::BaseRepresenter
      alias_method :modification, :represented

      def initialize(options)
        @material = options[:material]
        @latest_revision = options[:latest_revision]

        super(options[:modification])
      end

      link :vsm do |opts|
        opts[:url_builder].vsm_show_material_url({material_fingerprint: @material.getFingerprint(), revision: modification.revision})
      end

      property :userName, as: :user_name, skip_nil: true
      property :emailAddress, as: :email_address, skip_nil: true
      property :revision, skip_nil: true
      property :modifiedTime, as: :modified_time, skip_nil: true
      property :comment, skip_nil: true

    end
  end
end
