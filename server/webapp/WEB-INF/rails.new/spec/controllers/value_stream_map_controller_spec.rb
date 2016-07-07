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

require 'spec_helper'

describe ValueStreamMapController do

  before(:each)  do
    @value_stream_map_service = double('value_stream_map_service')
    @pipeline_service = double('pipeline_service')

    controller.stub(:value_stream_map_service).and_return(@value_stream_map_service)
    controller.stub(:pipeline_service).and_return(@pipeline_service)
    @result = HttpLocalizedOperationResult.new
    HttpLocalizedOperationResult.stub(:new).and_return(@result)
    @user = double('some user')
    controller.stub(:current_user).and_return(@user)
    controller.stub(:is_ie8?).and_return(false)

    @vsm_path_partial = proc do |name, counter|
      vsm_show_path(name, counter)
    end
    @vsm_material_path_partial = proc do |material_fingerprint, revision|
      vsm_show_material_path(material_fingerprint, revision)
    end
    @stage_detail_path_partial = proc do |pipeline_name, pipeline_counter, stage_name, stage_counter|
      stage_detail_tab_path(pipeline_name: pipeline_name, pipeline_counter: pipeline_counter, stage_name: stage_name, stage_counter: stage_counter)
    end
  end

  describe :redirect_to_stage_pdg_if_ie8 do
    before :each do
      @pipeline_history_service = double('pipeline history service')
      controller.stub(:pipeline_history_service).and_return(@pipeline_history_service)
    end

    it "should redirect to old pdg page when user is accessing via IE8" do
      controller.stub(:is_ie8?).and_return(true)
      pim = double('PIM')
      @pipeline_history_service.should_receive(:findPipelineInstance).with('foo', 42, @user, an_instance_of(HttpOperationResult)).and_return(pim)
      controller.stub(:url_for_pipeline_instance).with(pim).and_return('/some_funky_url')

      get :show, { pipeline_name: 'foo', pipeline_counter: '42' }

      expect(response.status).to eq(302)
      expect(response).to redirect_to("/some_funky_url")
    end

    it "should not redirect to old pdg page when user is accessing via IE8 but requesting format is not HTML" do
      controller.stub(:is_ie8?).and_return(true)
      controller.stub(:generate_vsm_json).and_return('some_json')
      @pipeline_history_service.should_receive(:findPipelineInstance).never
      @pipeline_service.should_receive(:findPipelineByCounterOrLabel).with('foo', '42').and_return('pipeline')

      get :show, { pipeline_name: 'foo', pipeline_counter: '42', format: 'json' }

      expect(response.status).to eq(200)
    end

    it "should not redirect to old pdg page when user is using a browser other than IE8" do
      controller.stub(:is_ie8?).and_return(false)
      @pipeline_history_service.should_receive(:findPipelineInstance).never
      @pipeline_service.should_receive(:findPipelineByCounterOrLabel).with('foo', '42').and_return('pipeline')

      get :show, { pipeline_name: 'foo', pipeline_counter: '42' }

      expect(response.status).to eq(200)
    end
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
      @pipeline_service.stub(:findPipelineByCounterOrLabel).with("foo","1").and_throw(Exception.new());
      get :show, pipeline_name: pipeline, pipeline_counter: 1

      expect(assigns(:pipeline)).to eq(nil)
    end

    describe "render json" do
      it "should get the pipeline dependency graph json" do
        pipeline = "P1"
        @pipeline_service.stub(:findPipelineByCounterOrLabel).with("P1", "1").and_return(nil)
        vsm = ValueStreamMap.new(pipeline, nil)
        vsm.addUpstreamNode(PipelineDependencyNode.new("git", "git"), nil, pipeline)
        model = vsm.presentationModel()
        @value_stream_map_service.should_receive(:getValueStreamMap).with(pipeline, 1, @user, @result).and_return(model)

        get :show, pipeline_name: pipeline, pipeline_counter: 1, format: "json"

        expect(response.status).to eq(200)

        expect(response.body).to eq(ValueStreamMapModel.new(model, nil, @l, @vsm_path_partial, @vsm_material_path_partial, @stage_detail_path_partial).to_json)
      end

      it "should render pipeline dependency graph JSON with pipeline instance and stage details" do
        @pipeline_service.stub(:findPipelineByCounterOrLabel).with("current", "1").and_return(nil)
        revision_p1_1 = PipelineRevision.new("p1", 1, "label-p1-1")
        revision_p1_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p1-1", "j1", "p1"), StageMother.passedStageInstance("stage-2-for-p1-1", "j2", "p1")]))
          modification = com.thoughtworks.go.domain.materials.Modification.new("user", "comment", "", java.util.Date.new() , "r1")
          modifications = com.thoughtworks.go.domain.materials.Modifications.new([modification].to_java(com.thoughtworks.go.domain.materials.Modification))
        scm_revision = SCMRevision.new(modification)
        pipeline = "current"
        vsm = ValueStreamMap.new(pipeline, nil)
        vsm.addUpstreamNode(PipelineDependencyNode.new("p1", "p1"), revision_p1_1, pipeline)
        vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git1", "http://git.com", "Git"),CaseInsensitiveString.new("git"), "p1", MaterialRevision.new(nil, false, modification))
        vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git2", "http://git.com", "Git"), nil, "p1", MaterialRevision.new(nil, false, modifications))
        model = vsm.presentationModel()
        @value_stream_map_service.should_receive(:getValueStreamMap).with(pipeline, 1,@user, @result).and_return(model)

        get :show, pipeline_name: pipeline, pipeline_counter: 1, format: "json"

        graph_details = JSON.parse(response.body)
        expected_graph_details = JSON.parse(expected_json_for_graph_with_pipeline_instance_details)
        graph_details.should == expected_graph_details
      end

      it "should display error message when the pipeline does not exist" do
        pipeline = "P1"
        @pipeline_service.stub(:findPipelineByCounterOrLabel).with("P1", "1").and_return(nil)

        @value_stream_map_service.should_receive(:getValueStreamMap) do |pipeline, pipeline_counter, user, result|
          result.stub(:message).with(anything).and_return("error")
        end

        get :show, pipeline_name: pipeline, pipeline_counter: 1, format: "json"

        expect(response.body).to eq({ error: "error" }.to_json)
      end
    end

    describe "render html" do
      it "should render html when html format" do
        @pipeline_service.stub(:findPipelineByCounterOrLabel).with("P1", "1").and_return(nil)
        get :show, pipeline_name: "P1", pipeline_counter: 1
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
                        "locator": "/pipelines/p1/1/stage-1-for-p1-1/1",
                        "status": "Passed"
                      },
                      {
                        "name": "stage-2-for-p1-1",
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
                "name": "p1"
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
                "name": "current"
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
        vsm.addDownstreamNode(PipelineDependencyNode.new("p1", "p1"), vsm.current_material.getId())
        model = vsm.presentationModel()
        @value_stream_map_service.should_receive(:getValueStreamMap).with(material.getFingerprint(), 'revision', @user, @result).and_return(model)

        get :show_material, material_fingerprint: material.getFingerprint(), revision: 'revision', format: "json"

        expect(response.status).to eq(200)

        expect(response.body).to eq(ValueStreamMapModel.new(model, nil, @l, @vsm_path_partial, @vsm_material_path_partial, @stage_detail_path_partial).to_json)
      end

      it "should display error message when the pipeline does not exist" do
        fingerprint = 'fingerprint'
        revision = 'revision'
        @value_stream_map_service.should_receive(:getValueStreamMap) do |fingerprint, revision, user, result|
          result.stub(:message).with(anything).and_return("error")
        end

        get :show_material, material_fingerprint: fingerprint, revision: revision, format: "json"

        expect(response.body).to eq({ error: "error" }.to_json)
      end
    end

    describe "render html" do
      it "should render html when html format" do
        get :show_material, material_fingerprint: "fingerprint", revision: 'revision'

        assert_template "show_material"
      end
    end
  end
end
