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

require 'rails_helper'

describe ValueStreamMapController do
  describe "show" do
    it "should route to pdg show path" do
      expect(:get => vsm_show_path(pipeline_name: "P", pipeline_counter: 1, format: "json")).to route_to(controller: "value_stream_map", action: 'show', pipeline_name: "P", pipeline_counter: "1", format: "json")
      expect(:get => vsm_show_path(pipeline_name: "P", pipeline_counter: 1, format: "html")).to route_to(controller: "value_stream_map", action: 'show', pipeline_name: "P", pipeline_counter: "1", format: :html)
      expect(:get => vsm_show_path(pipeline_name: "P", pipeline_counter: 1)).to route_to(controller: "value_stream_map", action: 'show', pipeline_name: "P", pipeline_counter: "1", format: :html)

      expect(get: "/pipelines/value_stream_map/name_of_pipeline/15.json").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name_of_pipeline", pipeline_counter: "15", format: "json" })
      expect(get: "/pipelines/value_stream_map/name_of_pipeline/15.html").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name_of_pipeline", pipeline_counter: "15", format: "html" })
      expect(get: "/pipelines/value_stream_map/name_of_pipeline/15").to route_to({ format: :html, controller: "value_stream_map", action: 'show', pipeline_name: "name_of_pipeline", pipeline_counter: "15" })
    end

    it "should route to pdg show path for pipelines with dot in their name" do
      expect(:get => vsm_show_path(pipeline_name: "P.Q", pipeline_counter: 1, format: "json")).to route_to("format"=>"json", "controller"=>"value_stream_map", "action"=>"show", "pipeline_name"=>"P.Q", "pipeline_counter"=>"1")
      expect(:get => vsm_show_path(pipeline_name: "P.Q", pipeline_counter: 1, format: "html")).to route_to("format"=>:html, "controller"=>"value_stream_map", "action"=>"show", "pipeline_name"=>"P.Q", "pipeline_counter"=>"1")
      expect(:get => vsm_show_path(pipeline_name: "P.Q", pipeline_counter: 1)).to route_to("format"=>:html, "controller"=>"value_stream_map", "action"=>"show", "pipeline_name"=>"P.Q", "pipeline_counter"=>"1")

      expect(get: "/pipelines/value_stream_map/name.of.pipeline/15.json").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name.of.pipeline", pipeline_counter: "15", format: "json" })
      expect(get: "/pipelines/value_stream_map/name.of.pipeline/15.html").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name.of.pipeline", pipeline_counter: "15", format: "html" })
      expect(get: "/pipelines/value_stream_map/name.of.pipeline/15").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name.of.pipeline", pipeline_counter: "15", format: :html })
    end
  end

  describe "show material" do
    it "should route to VSM show material path" do
      expect(:get => vsm_show_material_path(material_fingerprint: "fingerprint", revision: 'revision', format: "json")).to route_to({ controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision", format: "json" })
      expect(:get => vsm_show_material_path(material_fingerprint: "fingerprint", revision: 'revision', format: "html")).to route_to({ controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision", format: :html })
      expect(:get => vsm_show_material_path(material_fingerprint: "fingerprint", revision: 'revision')).to route_to({ controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision", format: :html })

      expect(get: "/materials/value_stream_map/fingerprint/revision.json").to route_to({ controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision", format: "json" })
      expect(get: "/materials/value_stream_map/fingerprint/revision.html").to route_to({ controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision", format: "html" })
      expect(get: "/materials/value_stream_map/fingerprint/revision").to route_to({ format: :html, controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision" })

      expect(get: "/materials/value_stream_map/fingerprint/revision.with.dots.json").to route_to({ format: "json", controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision.with.dots" })
      expect(get: "/materials/value_stream_map/fingerprint/revision.with.dots.html").to route_to({ format: "html", controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision.with.dots" })
      expect(get: "/materials/value_stream_map/fingerprint/revision.with.dots").to route_to({ format: :html, controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision.with.dots" })
    end
  end
end
