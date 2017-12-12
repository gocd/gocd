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

describe ApiV2::Dashboard::PipelineInstanceRepresenter do
  include PipelineModelMother

  it 'renders pipeline instance for pipeline that has never been executed with hal representation' do
    presenter = ApiV2::Dashboard::PipelineInstanceRepresenter.new(pipeline_instance_model_empty('p1', 's1'))

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:self, :compare_url, :history_url, :vsm_url)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/pipelines/p1/instance/0')
    expect(actual_json).to have_link(:compare_url).with_url('http://test.host/compare/p1/-1/with/0')
    expect(actual_json).to have_link(:history_url).with_url('http://test.host/api/pipelines/p1/history')
    expect(actual_json).to have_link(:vsm_url).with_url('http://test.host/pipelines/value_stream_map/p1/0')
    expect(actual_json.fetch(:_embedded)[:stages].collect { |s| s[:name] }).to eq(['s1'])
  end

  it 'renders all pipeline instance with hal representation' do
    instance = pipeline_instance_model({:name => "p1", :label => "g1", :counter => 5, :stages => [{:name => "cruise", :counter => "10", :approved_by => "Anonymous"}]})
    date = instance.getScheduledDate()
    presenter = ApiV2::Dashboard::PipelineInstanceRepresenter.new(instance)

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    actual_json.delete(:_links)
    actual_json.delete(:_embedded)
    expect(actual_json).to eq({:label => 'g1', :scheduled_at => date, :triggered_by => 'Triggered by Anonymous',
                               :build_cause => {:approver => 'anonymous', :is_forced => true, :trigger_message=>"Forced by anonymous",
                                                :material_revisions=>[] }})
  end

  it 'renders all pipeline instance with build_cause' do
    instance = pipeline_instance_model({:name => "p1", :label => "g1", :counter => 5, :stages => [{:name => "cruise", :counter => "10", :approved_by => "Anonymous"}]})
    material_revisions = ModificationsMother.createHgMaterialRevisions()
    pipeline_dependency = ModificationsMother.dependencyMaterialRevision("up1", 1, "label", "first", 1, Time.now)
    material_revisions.addRevision(pipeline_dependency)

    instance.setBuildCause(BuildCause.createWithModifications(material_revisions, 'Anonymous'))

    date = instance.getScheduledDate()
    presenter = ApiV2::Dashboard::PipelineInstanceRepresenter.new(instance)

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    actual_json.delete(:_links)
    actual_json.delete(:_embedded)

    expected_build_cause = {:approver => 'Anonymous', :is_forced => false, :trigger_message => 'modified by user2',
                            :material_revisions => [
                              {:material_type => 'Mercurial',
                               :material_name => 'hg-url',
                               :changed => false,
                               :modifications => [
                                 {
                                   :_links => {
                                     :vsm => {
                                       :href => 'http://test.host/materials/value_stream_map/4290e91721d0a0be34955725cfd754113588d9c27c39f9bd1a97c15e55832515/9fdcf27f16eadc362733328dd481d8a2c29915e1'
                                     }},
                                   :user_name => 'user2',
                                   :email_address => 'email2',
                                   :revision => '9fdcf27f16eadc362733328dd481d8a2c29915e1',
                                   :modified_time => material_revisions.first.getModifications().first.getModifiedTime(),
                                   :comment => 'comment2'
                                 },
                                 {
                                   :_links => {
                                     :vsm => {
                                       :href => 'http://test.host/materials/value_stream_map/4290e91721d0a0be34955725cfd754113588d9c27c39f9bd1a97c15e55832515/eef77acd79809fc14ed82b79a312648d4a2801c6'}
                                   },
                                   :user_name => 'user1',
                                   :email_address => 'email1',
                                   :revision => 'eef77acd79809fc14ed82b79a312648d4a2801c6',
                                   :modified_time => material_revisions.first.getModifications().last.getModifiedTime(),
                                   :comment => 'comment1'
                                 }]
                              },
                              {:material_type => "Pipeline",
                               :material_name => "up1",
                               :changed => false,
                               :modifications => [
                                 {:_links => {
                                   :vsm => {
                                     :href => 'http://test.host/pipelines/value_stream_map/up1/1'
                                   },
                                   :stage_details_url=> {
                                       :href=>'http://test.host/pipelines/up1/1/first/1'
                                   }
                                 },
                                  :revision => 'up1/1/first/1',
                                  :modified_time => material_revisions.getMaterialRevision(1).getModifications().first.getModifiedTime(),
                                  :pipeline_label => 'label'
                                 }
                               ]}
                            ]
    }

    expect(actual_json).to eq({:label => 'g1', :scheduled_at => date, :triggered_by => 'Triggered by Anonymous', :build_cause => expected_build_cause})
  end

end