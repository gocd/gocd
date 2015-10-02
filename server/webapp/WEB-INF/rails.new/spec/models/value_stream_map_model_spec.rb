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


describe ValueStreamMapModel do

  before :each do
    @result = double("HttpLocalizedOperationResult")
    @l = Spring.bean("localizer")
  end

  it "should initialize graph model correctly" do
    @result.stub(:isSuccessful).and_return(true)
    # git -> p1 -> current
    #  +---- X -----^
    #
    vsm = ValueStreamMap.new("current", PipelineRevision.new("current", 1, "current-1"))
    vsm.addUpstreamNode(PipelineDependencyNode.new("p1", "p1"), PipelineRevision.new("p1", 1, "p1-1"), "current")
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), CaseInsensitiveString.new("git1"), modifications(), "p1")
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), CaseInsensitiveString.new("git2"), modifications(), "current")

    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil, @l)
    materialNames = Array.new
    materialNames << "git1"
    materialNames << "git2"

    graph_model.current_pipeline.should == "current"
    graph_model.current_material.should == nil

    graph_model.levels.size.should == 3
    graph_model.levels[0].nodes.size.should == 1
    graph_model.levels[1].nodes.size.should == 2
    graph_model.levels[2].nodes.size.should == 1

    nodeInThirdLevel = graph_model.levels[0].nodes[0]
    nodeInThirdLevel.id.should == "git"
    nodeInThirdLevel.node_type.should == "GIT"
    nodeInThirdLevel.material_names.should == materialNames
    nodeInThirdLevel.locator.should == ""
    nodeInThirdLevel.dependents[0].should == "p1"
    nodeInThirdLevel.dependents.size.should == 2
    nodeInThirdLevel.parents.size.should == 0
    nodeInThirdLevel.depth.should == 1

    node1InSecondLevel = graph_model.levels[1].nodes[0]
    node1InSecondLevel.id.should == "p1"
    node1InSecondLevel.node_type.should == DependencyNodeType::PIPELINE.to_s
    node1InSecondLevel.locator.should == "/go/tab/pipeline/history/p1"
    node1InSecondLevel.dependents[0].should == "current"
    node1InSecondLevel.parents[0].should == "git"
    node1InSecondLevel.depth.should == 1

    node2InSecondLevel = graph_model.levels[1].nodes[1]
    node2InSecondLevel.node_type.should == DependencyNodeType::DUMMY.to_s
    node2InSecondLevel.locator.should == ""
    node2InSecondLevel.name.start_with?("dummy").should == true
    node2InSecondLevel.dependents[0].should == "current"
    node2InSecondLevel.parents[0].should == "git"
    node2InSecondLevel.depth.should == 2

    nodeInFirstLevel = graph_model.levels[2].nodes[0]
    nodeInFirstLevel.id.should == "current"
    nodeInFirstLevel.node_type.should == DependencyNodeType::PIPELINE.to_s
    nodeInFirstLevel.locator.should == "/go/tab/pipeline/history/current"
    nodeInFirstLevel.dependents.size.should == 0
    nodeInFirstLevel.parents[0].should == "p1"
    nodeInFirstLevel.depth.should == 1
  end

  it "should set error on model if result was not successful" do
    graph_model = ValueStreamMapModel.new(nil, "error message", @l)
    graph_model.current_pipeline.should == nil
    graph_model.levels.should == nil
    graph_model.error.should == "error message"
  end

  it "should initialize graph model with instances for pipelines" do
    # git -> p1 --->p2---> current ---> p3
    #        |            ^
    #        |            |
    #        -------X-----

    revision_p1_1 = PipelineRevision.new("p1", 1, "label-p1-1")
    revision_p1_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p1-1", "j1", "p1"), StageMother.passedStageInstance("stage-2-for-p1-1", "j2", "p1")]))

    revision_p1_2 = PipelineRevision.new("p1", 2, "label-p1-2")
    revision_p1_2.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p1-2", "j1", "p1")]))

    revision_p2_1 = PipelineRevision.new("p2", 1, "label-p2-1")
    revision_p2_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p2-1", "j1", "p2"), StageMother.unrunStage("unrun_stage")]))

    revision_p3_1 = UnrunPipelineRevision.new("p3")
    revision_p3_1.addStages(Stages.new([StageMother.unrunStage("unrun_stage1"), StageMother.unrunStage("unrun_stage2")]))

    vsm = ValueStreamMap.new("current", nil)
    vsm.addUpstreamNode(PipelineDependencyNode.new("p1", "p1"), revision_p1_1, "current")
    vsm.addUpstreamNode(PipelineDependencyNode.new("p2", "p2"), revision_p2_1, "current")
    vsm.addUpstreamNode(PipelineDependencyNode.new("p1", "p1"), revision_p1_2, "p2")
    modifications = modifications()
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), com.thoughtworks.go.config.CaseInsensitiveString.new("git-trunk"), modifications, "p1")
    p3_node = vsm.addDownstreamNode(PipelineDependencyNode.new("p3", "p3"), "current");
    p3_node.addRevision(revision_p3_1);

    vsm_path_partial = proc do |pipeline_name, counter|
      "some/path/to/#{pipeline_name}/#{counter}"
    end
    vsm_material_path_partial = proc do |fingerprint, revision|
      "some/path/to/#{fingerprint}/#{revision}"
    end
    stage_detail_path_partial = proc do |pipeline_name, counter, stage_name, stage_counter|
      "path/to/stage/#{pipeline_name}/#{counter}/#{stage_name}/#{stage_counter}"
    end
    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil, @l, vsm_path_partial, vsm_material_path_partial, stage_detail_path_partial)

    nodeThatTheGraphIsBuiltFor = graph_model.levels[3].nodes[0]
    nodeThatTheGraphIsBuiltFor.id.should == "current"

    nodeP1 = graph_model.levels[1].nodes[0]
    nodeP1.id.should == "p1"
    nodeP1.instances.size.should == 2
    nodeP1.instances[0].label.should == "label-p1-2"
    nodeP1.instances[0].counter.should == 2
    nodeP1.instances[0].locator.should == "some/path/to/p1/2"
    nodeP1.instances[0].stages.size.should == 1
    nodeP1.instances[0].stages[0].name.should == "stage-1-for-p1-2"
    nodeP1.instances[0].stages[0].status.should == "Passed"

    nodeP1.instances[1].label.should == "label-p1-1"
    nodeP1.instances[1].counter.should == 1
    nodeP1.instances[1].locator.should == "some/path/to/p1/1"
    nodeP1.instances[1].stages.size.should == 2
    nodeP1.instances[1].stages[0].name.should == "stage-1-for-p1-1"
    nodeP1.instances[1].stages[0].status.should == "Passed"
    nodeP1.instances[1].stages[0].locator.should == "path/to/stage/p1/1/stage-1-for-p1-1/1"
    nodeP1.instances[1].stages[1].name.should == "stage-2-for-p1-1"
    nodeP1.instances[1].stages[1].status.should == "Passed"
    nodeP1.instances[1].stages[1].locator.should == "path/to/stage/p1/1/stage-2-for-p1-1/1"

    nodeP2 = graph_model.levels[2].nodes[0]
    nodeP2.id.should == "p2"
    nodeP2.instances.size.should == 1
    nodeP2.instances[0].label.should == "label-p2-1"
    nodeP2.instances[0].counter.should == 1
    nodeP2.instances[0].locator.should == "some/path/to/p2/1"
    nodeP2.instances[0].stages.size.should == 2
    nodeP2.instances[0].stages[0].name.should == "stage-1-for-p2-1"
    nodeP2.instances[0].stages[0].status.should == "Passed"
    nodeP2.instances[0].stages[0].locator.should == "path/to/stage/p2/1/stage-1-for-p2-1/1"
    nodeP2.instances[0].stages[1].name.should == "unrun_stage"
    nodeP2.instances[0].stages[1].status.should == "Unknown"
    nodeP2.instances[0].stages[1].locator.should == ""

    nodeP3 = graph_model.levels[4].nodes[0]
    nodeP3.id.should == "p3"
    nodeP3.instances.size.should == 1
    nodeP3.instances[0].label.should == ""
    nodeP3.instances[0].counter.should == 0
    nodeP3.instances[0].locator.should == ""
    nodeP3.instances[0].stages[0].name.should == "unrun_stage1"
    nodeP3.instances[0].stages[0].status.should == "Unknown"
    nodeP3.instances[0].stages[0].locator.should == ""
    nodeP3.instances[0].stages[1].name.should == "unrun_stage2"
    nodeP3.instances[0].stages[1].status.should == "Unknown"
    nodeP3.instances[0].stages[1].locator.should == ""


    nodeDummy = graph_model.levels[2].nodes[1]
    nodeDummy.node_type.should == DependencyNodeType::DUMMY.to_s
    nodeDummy.instances.size.should == 0

    nodeGit = graph_model.levels[0].nodes[0]
    nodeGit.id.should == "git"
    nodeGit.node_type.should == "GIT"
    nodeGit.instances.size.should == 1
    git_instance = nodeGit.instances[0]
    modification = modifications.get(0)
    git_instance.revision.should == modification.getRevision()
    git_instance.locator.should == "some/path/to/git/r1"
  end


  it "should populate message & locator correctly" do
    # git -> p1 --->p2---> current ---> p3
    #        |            ^
    #        |            |
    #        -------X-----

    revision_p1_1 = PipelineRevision.new("p1", 1, "label-p1-1")
    revision_p1_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p1-1", "j1", "p1"), StageMother.passedStageInstance("stage-2-for-p1-1", "j2", "p1")]))

    revision_p2_1 = PipelineRevision.new("p2", 1, "label-p2-1")
    revision_p2_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p2-1", "j1", "p2"), StageMother.unrunStage("unrun_stage")]))

    vsm = ValueStreamMap.new("current", nil)
    vsm.addUpstreamNode(PipelineDependencyNode.new("p1", "p1"), revision_p1_1, "current")
    vsm.addUpstreamNode(PipelineDependencyNode.new("p2", "p2"), revision_p2_1, "current")
    vsm.addUpstreamNode(PipelineDependencyNode.new("p1", "p1"), revision_p1_1, "p2")
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), com.thoughtworks.go.config.CaseInsensitiveString.new("git-trunk"), modifications(), "p1")

    p3_node = vsm.addDownstreamNode(PipelineDependencyNode.new("p3", "p3"), "current");
    p3_node.setViewType(com.thoughtworks.go.domain.valuestreammap.VSMViewType::NO_PERMISSION)
    p3_node.setMessage(com.thoughtworks.go.i18n.LocalizedMessage.string("VSM_PIPELINE_UNAUTHORIZED", [].to_java(java.lang.Object)))


    vsm_path_partial = proc do |pipeline_name, counter|
      "some/path/to/#{pipeline_name}/#{counter}"
    end
    vsm_material_path_partial = proc do |fingerprint, revision|
      "some/path/to/#{fingerprint}/#{revision}"
    end
    stage_detail_path_partial = proc do |pipeline_name, counter, stage_name, stage_counter|
      "path/to/stage/#{pipeline_name}/#{counter}/#{stage_name}/#{stage_counter}"
    end
    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil, @l, vsm_path_partial, vsm_material_path_partial, stage_detail_path_partial)

    nodeThatTheGraphIsBuiltFor = graph_model.levels[3].nodes[0]
    nodeThatTheGraphIsBuiltFor.id.should == "current"

    nodeP1 = graph_model.levels[1].nodes[0]
    nodeP1.id.should == "p1"
    nodeP1.instances.size.should == 1
    nodeP1.instances[0].label.should == "label-p1-1"
    nodeP1.instances[0].counter.should == 1
    nodeP1.instances[0].locator.should == "some/path/to/p1/1"
    nodeP1.instances[0].stages.size.should == 2
    nodeP1.instances[0].stages[0].name.should == "stage-1-for-p1-1"
    nodeP1.instances[0].stages[0].status.should == "Passed"

    nodeP2 = graph_model.levels[2].nodes[0]
    nodeP2.id.should == "p2"
    nodeP2.instances.size.should == 1
    nodeP2.instances[0].label.should == "label-p2-1"
    nodeP2.instances[0].counter.should == 1
    nodeP2.instances[0].locator.should == "some/path/to/p2/1"
    nodeP2.instances[0].stages.size.should == 2
    nodeP2.instances[0].stages[0].name.should == "stage-1-for-p2-1"
    nodeP2.instances[0].stages[0].status.should == "Passed"
    nodeP2.instances[0].stages[0].locator.should == "path/to/stage/p2/1/stage-1-for-p2-1/1"
    nodeP2.instances[0].stages[1].name.should == "unrun_stage"
    nodeP2.instances[0].stages[1].status.should == "Unknown"
    nodeP2.instances[0].stages[1].locator.should == ""

    nodeP3 = graph_model.levels[4].nodes[0]
    nodeP3.id.should == "p3"
    nodeP3.instances.size.should == 0
    nodeP3.view_type.should == "NO_PERMISSION"
    nodeP3.locator.should == ""
    nodeP3.message.should == "You are not authorized to view this pipeline"

    nodeDummy = graph_model.levels[2].nodes[1]
    nodeDummy.node_type.should == DependencyNodeType::DUMMY.to_s
    nodeDummy.instances.size.should == 0

    nodeGit = graph_model.levels[0].nodes[0]
    nodeGit.id.should == "git"
    nodeGit.node_type.should == "GIT"
    nodeGit.instances.size.should == 1
    nodeGit.instances[0].revision.should == "r1"
    nodeGit.instances[0].locator.should == "some/path/to/git/r1"
  end

  it "should populate details of material modification like revision, user, comment and modified_time" do
    # git -> current

    vsm = ValueStreamMap.new("current", PipelineRevision.new("current", 1, "current-1"))
    modifications = modifications()
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), CaseInsensitiveString.new("git1"), modifications, "current")
    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil, @l)
    git_node = graph_model.levels[0].nodes[0]
    git_node.instances.size.should == 1

    git_instance = git_node.instances[0]
    modification = modifications.get(0)
    git_instance.revision.should == modification.getRevision()
    git_instance.user.should == modification.getUserName()
    git_instance.comment.should == modification.getComment()
    git_instance.modified_time.should == "less than a minute ago"
  end

  it "should create VSM json model for material correctly" do
    # git -> p1

    material = GitMaterial.new("url")
    modifications = modifications()
    vsm = ValueStreamMap.new(material, nil, modifications[0])
    vsm.addDownstreamNode(PipelineDependencyNode.new("p1", "p1"), vsm.current_material.getId())

    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil, @l)
    graph_model.current_pipeline.should == nil
    graph_model.current_material.should == material.getFingerprint()

    git_node = graph_model.levels[0].nodes[0]
    git_node.instances.size.should == 1

    git_instance = git_node.instances[0]
    modification = modifications.get(0)
    git_instance.revision.should == modification.getRevision()
    git_instance.user.should == modification.getUserName()
    git_instance.comment.should == modification.getComment()
    git_instance.modified_time.should == "less than a minute ago"
  end

  def modifications
    modification = com.thoughtworks.go.domain.materials.Modification.new("user", "comment", "", java.util.Date.new(), "r1")
    return com.thoughtworks.go.domain.materials.Modifications.new([modification].to_java(com.thoughtworks.go.domain.materials.Modification))
  end

end
