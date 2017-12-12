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

describe ApiV2::Dashboard::PipelineRepresenter do
  include PipelineModelMother

  it 'renders pipeline with hal representation' do
    counter = double('Counter')
    counter.stub(:getNext).and_return(1)
    permissions = Permissions.new(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE)
    pipeline    = GoDashboardPipeline.new(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter)
    presenter   = ApiV2::Dashboard::PipelineRepresenter.new({pipeline: pipeline, user: Username.new(CaseInsensitiveString.new(SecureRandom.hex))})
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc, :settings_path, :trigger, :trigger_with_options, :unpause, :pause)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/pipelines/pipeline_name/history')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/current/#pipelines')
    expect(actual_json).to have_link(:settings_path).with_url('http://test.host/admin/pipelines/pipeline_name/general')
    expect(actual_json).to have_link(:trigger).with_url('http://test.host/api/pipelines/pipeline_name/schedule')
    expect(actual_json).to have_link(:trigger_with_options).with_url('http://test.host/api/pipelines/pipeline_name/schedule')
    expect(actual_json).to have_link(:unpause).with_url('http://test.host/api/pipelines/pipeline_name/unpause')
    expect(actual_json).to have_link(:pause).with_url('http://test.host/api/pipelines/pipeline_name/pause')
    actual_json.delete(:_links)
    expect(actual_json.delete(:_embedded)).to  eq({:instances => [expected_embedded_pipeline(presenter.instances.first)]})
    expect(actual_json).to eq(pipelines_hash)
  end

  describe 'authorization' do
    it 'user can operate a pipeline if user is pipeline_level operator' do
      counter = double('Counter')
      counter.stub(:getNext).and_return(1)
      permissions = Permissions.new(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE)
      pipeline    = GoDashboardPipeline.new(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter)
      presenter   = ApiV2::Dashboard::PipelineRepresenter.new({pipeline: pipeline, user: Username.new(CaseInsensitiveString.new(SecureRandom.hex))})
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      actual_json.delete(:_links)
      actual_json.delete(:_embedded)
      expect(actual_json).to eq(pipelines_hash.merge!(can_operate: true))
    end

    it 'user can administer a pipeline if user is admin of pipeline' do
      counter = double('Counter')
      counter.stub(:getNext).and_return(1)
      permissions = Permissions.new(NoOne.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE, NoOne.INSTANCE)
      pipeline    = GoDashboardPipeline.new(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter)
      presenter   = ApiV2::Dashboard::PipelineRepresenter.new({pipeline: pipeline, user: Username.new(CaseInsensitiveString.new(SecureRandom.hex))})
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      actual_json.delete(:_links)
      actual_json.delete(:_embedded)
      expect(actual_json).to eq(pipelines_hash.merge!(can_administer: true))
    end

    it 'user can unlock and pause a pipeline if user is operator of pipeline' do
      counter = double('Counter')
      counter.stub(:getNext).and_return(1)
      permissions = Permissions.new(NoOne.INSTANCE, Everyone.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE)
      pipeline    = GoDashboardPipeline.new(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter)
      presenter   = ApiV2::Dashboard::PipelineRepresenter.new({pipeline: pipeline, user: Username.new(CaseInsensitiveString.new(SecureRandom.hex))})
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      actual_json.delete(:_links)
      actual_json.delete(:_embedded)
      expect(actual_json).to eq(pipelines_hash.merge!(can_pause: true, can_unlock: true))
    end
  end


  private

  def expected_embedded_pipeline(pipeline_model)
    ApiV2::Dashboard::PipelineInstanceRepresenter.new(pipeline_model).to_hash(url_builder: UrlBuilder.new)
  end

  def pipelines_hash
    {
      name:       'pipeline_name',
      locked:     false,
      last_updated_timestamp: 1,
      pause_info: {
        paused:    false,
        paused_by:    nil,
        pause_reason: nil
      },
      can_operate: false,
      can_administer: false,
      can_unlock: false,
      can_pause: false
    }
  end

end
