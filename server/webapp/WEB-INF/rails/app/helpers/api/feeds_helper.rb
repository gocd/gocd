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

module Api
  module FeedsHelper
    include JavaImports

    def pipeline_details_url(stage_locator, pipeline_id) #FIXME: this is a horrible name, we should name this better, especially considering this is a helper method
      stage_identifier = StageIdentifier.new(stage_locator)
      api_pipeline_instance_url :name => stage_identifier.getPipelineName(), :id => pipeline_id
    end

    def pretty_print_xml(doc)
      stream = ByteArrayOutputStream.new
      XMLWriter.new(stream, OutputFormat.createPrettyPrint()).write(doc)
      stream.toString()
    end
  end
end
