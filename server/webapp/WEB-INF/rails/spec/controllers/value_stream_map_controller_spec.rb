#
# Copyright 2019 ThoughtWorks, Inc.
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

require 'rails_helper'

describe ValueStreamMapController do

  before(:each)  do
    @value_stream_map_service = double('value_stream_map_service')
    @pipeline_service = double('pipeline_service')
    @material_config_service = double('material_config_service')

    allow(controller).to receive(:value_stream_map_service).and_return(@value_stream_map_service)
    allow(controller).to receive(:pipeline_service).and_return(@pipeline_service)
    allow(controller).to receive(:material_config_service).and_return(@material_config_service)
    @result = double(HttpLocalizedOperationResult)
    allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)
    @user = double('some user')
    allow(controller).to receive(:current_user).and_return(@user)
    allow(controller).to receive(:is_ie8?).and_return(false)
    allow(@result).to receive(:isSuccessful).and_return(true)
    allow(@result).to receive(:message).and_return(nil)
    @vsm_path_partial = proc do |name, counter|
      vsm_show_path(name, counter)
    end
    @vsm_material_path_partial = proc do |material_fingerprint, revision|
      vsm_show_material_path(material_fingerprint, revision)
    end
    @stage_detail_path_partial = proc do |pipeline_name, pipeline_counter, stage_name, stage_counter|
      stage_detail_tab_path_for(pipeline_name: pipeline_name, pipeline_counter: pipeline_counter, stage_name: stage_name, stage_counter: stage_counter)
    end
    @pipeline_edit_path_normal_edit = proc { |pipeline_name | pipeline_edit_path(:pipeline_name => pipeline_name, :current_tab => 'general') }
  end

  describe "show" do
    it "should route to pdg show path" do
      expect(controller.send(:vsm_show_path, { pipeline_name: "P", pipeline_counter: 1, format: "json" })).to eq("/pipelines/value_stream_map/P/1.json")
      expect(controller.send(:vsm_show_path, { pipeline_name: "P", pipeline_counter: 1, format: "html" })).to eq("/pipelines/value_stream_map/P/1")
      expect(controller.send(:vsm_show_path, { pipeline_name: "P", pipeline_counter: 1 })).to eq("/pipelines/value_stream_map/P/1")

      expect(get: "/pipelines/value_stream_map/name_of_pipeline/15.json").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name_of_pipeline", pipeline_counter: "15", format: "json" })
      expect(get: "/pipelines/value_stream_map/name_of_pipeline/15.html").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name_of_pipeline", pipeline_counter: "15", format: "html" })
      expect(get: "/pipelines/value_stream_map/name_of_pipeline/15").to route_to({ format: :html, controller: "value_stream_map", action: 'show', pipeline_name: "name_of_pipeline", pipeline_counter: "15" })
    end

    it "should route to pdg show path for pipelines with dot in their name" do
      expect(controller.send(:vsm_show_path, { pipeline_name: "P.Q", pipeline_counter: 1, format: "json" })).to eq("/pipelines/value_stream_map/P.Q/1.json")
      expect(controller.send(:vsm_show_path, { pipeline_name: "P.Q", pipeline_counter: 1, format: "html" })).to eq("/pipelines/value_stream_map/P.Q/1")
      expect(controller.send(:vsm_show_path, { pipeline_name: "P.Q", pipeline_counter: 1 })).to eq("/pipelines/value_stream_map/P.Q/1")

      expect(get: "/pipelines/value_stream_map/name.of.pipeline/15.json").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name.of.pipeline", pipeline_counter: "15", format: "json" })
      expect(get: "/pipelines/value_stream_map/name.of.pipeline/15.html").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name.of.pipeline", pipeline_counter: "15", format: "html" })
      expect(get: "/pipelines/value_stream_map/name.of.pipeline/15").to route_to({ controller: "value_stream_map", action: 'show', pipeline_name: "name.of.pipeline", pipeline_counter: "15", format: :html })
    end

    it "should show Error message when pipeline name and counter cannot be resolved to a unique instance" do
      pipeline = "foo"
      allow(@pipeline_service).to receive(:findPipelineByNameAndCounter).with("foo", 1).and_throw(Exception.new());
      get :show, params:{pipeline_name: pipeline, pipeline_counter: 1}

      expect(assigns(:pipeline)).to eq(nil)
    end

    describe "render json" do
      it "should get the pipeline dependency graph json" do
        pipeline = "P1"
        allow(@pipeline_service).to receive(:findPipelineByNameAndCounter).with("P1", 1).and_return(nil)
        vsm = ValueStreamMap.new(CaseInsensitiveString.new(pipeline), nil)
        vsm.addUpstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("git"), "git"), nil, CaseInsensitiveString.new(pipeline))
        model = vsm.presentationModel()
        expect(@value_stream_map_service).to receive(:getValueStreamMap).with(CaseInsensitiveString.new(pipeline), 1, @user, @result).and_return(model)

        get :show, params:{pipeline_name: pipeline, pipeline_counter: 1, format: "json"}

        expect(response.status).to eq(200)

        expect(response.body).to eq(ValueStreamMapModel.new(model, nil, @vsm_path_partial, @vsm_material_path_partial, @stage_detail_path_partial, @pipeline_edit_path_normal_edit).to_json)
      end

      it "should render pipeline dependency graph JSON with pipeline instance and stage details" do
        allow(@pipeline_service).to receive(:findPipelineByNameAndCounter).with("current", 1).and_return(nil)
        revision_p1_1 = PipelineRevision.new("p1", 1, "label-p1-1")
        revision_p1_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p1-1", "j1", "p1"), StageMother.passedStageInstance("stage-2-for-p1-1", "j2", "p1")]))
          modification = com.thoughtworks.go.domain.materials.Modification.new("user", "comment", "", java.util.Date.new() , "r1")
          modifications = com.thoughtworks.go.domain.materials.Modifications.new([modification].to_java(com.thoughtworks.go.domain.materials.Modification))
        scm_revision = SCMRevision.new(modification)
        pipeline = "current"
        vsm = ValueStreamMap.new(CaseInsensitiveString.new(pipeline), nil)
        vsm.addUpstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p1"), "p1"), revision_p1_1, CaseInsensitiveString.new(pipeline))
        vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git1", "http://git.com", "Git"),CaseInsensitiveString.new("git"), CaseInsensitiveString.new("p1"), MaterialRevision.new(nil, false, modification))
        vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git2", "http://git.com", "Git"), nil, CaseInsensitiveString.new("p1"), MaterialRevision.new(nil, false, modifications))
        model = vsm.presentationModel()
        expect(@value_stream_map_service).to receive(:getValueStreamMap).with(CaseInsensitiveString.new(pipeline), 1,@user, @result).and_return(model)

        get :show, params:{pipeline_name: pipeline, pipeline_counter: 1, format: "json"}

        graph_details = JSON.parse(response.body)
        expected_graph_details = JSON.parse(expected_json_for_graph_with_pipeline_instance_details)
        expect(graph_details).to eq(expected_graph_details)
      end

      it "should display error message when the pipeline does not exist" do
        pipeline = "P1"
        allow(@pipeline_service).to receive(:findPipelineByNameAndCounter).with("P1", 1).and_return(nil)

        expect(@value_stream_map_service).to receive(:getValueStreamMap) do |pipeline, pipeline_counter, user, result|
          allow(result).to receive(:message).and_return("error")
        end

        get :show, params:{pipeline_name: pipeline, pipeline_counter: 1, format: "json"}

        expect(response.body).to eq({ error: "error" }.to_json)
      end
    end

    describe "render html" do
      it "should render html when html format" do
        allow(@pipeline_service).to receive(:findPipelineByNameAndCounter).with("P1", 1).and_return(nil)
        get :show, params:{pipeline_name: "P1", pipeline_counter: 1}
        assert_template "show"
      end
    end

    def expected_json_for_graph_with_pipeline_instance_details
      %q!{
        "levels": [
          {
            "nodes": [
              {
                "id": "git1",
                "parents": [],
                "locator": "",
                "depth": 1,
                "instances": [],
                "material_revisions": [
                  {
                  "modifications": [
                    {
                     "comment": "comment",
                     "revision": "r1",
                     "user": "user",
                     "modified_time": "less than a minute ago",
                     "locator": "/materials/value_stream_map/git1/r1"
                    }
                  ]
                 }
                ],
                "dependents": [
                  "p1"
                ],
                "node_type": "GIT",
                "name": "http://git.com",
                "material_names": ["git"]
              },
              {
                "id": "git2",
                "parents": [],
                "locator": "",
                "depth": 2,
                "instances":[],
                "material_revisions": [
                  {
                  "modifications": [
                  {
                    "comment": "comment",
                    "revision": "r1",
                    "user": "user",
                    "modified_time": "less than a minute ago",
                    "locator": "/materials/value_stream_map/git2/r1"
                  }
                 ]
                }
                ],
                "dependents": [
                  "p1"
                ],
                "node_type": "GIT",
                "name": "http://git.com"
              }
            ]
          },
          {
            "nodes": [
              {
                "id": "p1",
                "parents": [
                  "git1",
                  "git2"
                ],
                "locator": "/go/tab/pipeline/history/p1",
                "depth": 1,

                "instances": [
                  {
                    "stages": [
                      {
                        "name": "stage-1-for-p1-1",
                        "duration": 0,
                        "locator": "/pipelines/p1/1/stage-1-for-p1-1/1",
                        "status": "Passed"
                      },
                      {
                        "name": "stage-2-for-p1-1",
                        "duration": 0,
                        "locator": "/pipelines/p1/1/stage-2-for-p1-1/1",
                        "status": "Passed"
                      }
                    ],
                    "locator": "/pipelines/value_stream_map/p1/1",
                    "counter": 1,
                    "label": "label-p1-1"
                  }
                ],
                "dependents": [
                  "current"
                ],
                "node_type": "PIPELINE",
                "name": "p1",
                "can_edit": false,
                "edit_path": "/admin/pipelines/p1/general"
              }
            ]
          },
          {
            "nodes": [
              {
                "id": "current",
                "parents": [
                  "p1"
                ],
                "locator": "/go/tab/pipeline/history/current",
                "depth": 1,
                "instances": [],
                "dependents": [],
                "node_type": "PIPELINE",
                "name": "current",
                "can_edit": false,
                "edit_path": "/admin/pipelines/current/general"
              }
            ]
          }
        ],
        "current_pipeline": "current"
      }!
    end
  end

  describe "show material" do
    it "should route to VSM show material path" do
      expect(controller.send(:vsm_show_material_path, { material_fingerprint: "fingerprint", revision: 'revision', format: "json" })).to eq("/materials/value_stream_map/fingerprint/revision.json")
      expect(controller.send(:vsm_show_material_path, { material_fingerprint: "fingerprint", revision: 'revision', format: "html" })).to eq("/materials/value_stream_map/fingerprint/revision")
      expect(controller.send(:vsm_show_material_path, { material_fingerprint: "fingerprint", revision: 'revision' })).to eq("/materials/value_stream_map/fingerprint/revision")

      expect(get: "/materials/value_stream_map/fingerprint/revision.json").to route_to({ controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision", format: "json" })
      expect(get: "/materials/value_stream_map/fingerprint/revision.html").to route_to({ controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision", format: "html" })
      expect(get: "/materials/value_stream_map/fingerprint/revision").to route_to({ format: :html, controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision" })

      expect(get: "/materials/value_stream_map/fingerprint/revision.with.dots.json").to route_to({ format: "json", controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision.with.dots" })
      expect(get: "/materials/value_stream_map/fingerprint/revision.with.dots.html").to route_to({ format: "html", controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision.with.dots" })
      expect(get: "/materials/value_stream_map/fingerprint/revision.with.dots").to route_to({ format: :html, controller: "value_stream_map", action: 'show_material', material_fingerprint: "fingerprint", revision: "revision.with.dots" })
    end

    describe "render json" do
      it "should get the pipeline dependency graph json" do
        material = GitMaterial.new("url")
        vsm = ValueStreamMap.new(material, nil, com.thoughtworks.go.domain.materials.Modification.new("user", "comment", "", java.util.Date.new() , "r1"))
        vsm.addDownstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p1"), "p1"), vsm.current_material.getId())
        model = vsm.presentationModel()
        expect(@value_stream_map_service).to receive(:getValueStreamMap).with(material.getFingerprint(), 'revision', @user, @result).and_return(model)

        get :show_material, params:{material_fingerprint: material.getFingerprint(), revision: 'revision', format: "json"}

        expect(response.status).to eq(200)

        expect(response.body).to eq(ValueStreamMapModel.new(model, nil, @vsm_path_partial, @vsm_material_path_partial, @stage_detail_path_partial, @pipeline_edit_path_normal_edit).to_json)
      end

      it "should display error message when the pipeline does not exist" do
        fingerprint = 'fingerprint'
        revision = 'revision'
        expect(@value_stream_map_service).to receive(:getValueStreamMap) do |fingerprint, revision, user, result|
          allow(result).to receive(:message).and_return("error")
        end

        get :show_material, params:{material_fingerprint: fingerprint, revision: revision, format: "json"}

        expect(response.body).to eq({ error: "error" }.to_json)
      end
    end

    describe "render html" do
      it "should render html when html format" do
        material_config = com.thoughtworks.go.helper.MaterialConfigsMother.git('http://some.repo')

        expect(@user).to receive(:getUsername).and_return(CaseInsensitiveString.new('some_user'))
        expect(@material_config_service).to receive(:getMaterialConfig).with('some_user', 'fingerprint', anything()).and_return(material_config)

        get :show_material, params:{material_fingerprint: 'fingerprint', revision: 'revision'}

        assert_template "show_material"
        expect(assigns(:material_display_name)).to eq('http://some.repo')
      end

      it 'should not assign material_display_name in absence of material for a given fingerprint' do
        expect(@user).to receive(:getUsername).and_return(CaseInsensitiveString.new('some_user'))
        expect(@material_config_service).to receive(:getMaterialConfig).with('some_user', 'fingerprint', anything()).and_return(nil)

        get :show_material, params:{material_fingerprint: 'fingerprint', revision: 'revision'}

        assert_template "show_material"
        expect(assigns(:material_display_name)).to be_nil
      end
    end
  end
end
