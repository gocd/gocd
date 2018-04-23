##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

require 'rails_helper'

describe Api::PipelinesController do
  include ApiHeaderSetupForRouting

  describe "history" do

    it "should route to history" do
      expect(:get => '/api/pipelines/up42/history').to route_to(:controller => "api/pipelines", :action => "history", :pipeline_name => 'up42', :offset => '0', :no_layout => true)
      expect(:get => '/api/pipelines/up42/history/1').to route_to(:controller => "api/pipelines", :action => "history", :pipeline_name => 'up42', :offset => '1', :no_layout => true)
    end

    describe "with_pipeline_name_contraint" do
      it 'should route to history action of pipelines controller having dots in pipeline name' do
        expect(:get => 'api/pipelines/some.thing/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: 'some.thing', :offset => '0')
      end

      it 'should route to history action of pipelines controller having hyphen in pipeline name' do
        expect(:get => 'api/pipelines/some-thing/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: 'some-thing', :offset => '0')
      end

      it 'should route to history action of pipelines controller having underscore in pipeline name' do
        expect(:get => 'api/pipelines/some_thing/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: 'some_thing', :offset => '0')
      end

      it 'should route to history action of pipelines controller having alphanumeric pipeline name' do
        expect(:get => 'api/pipelines/123foo/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: '123foo', :offset => '0')
      end

      it 'should route to history action of pipelines controller having capitalized pipeline name' do
        expect(:get => 'api/pipelines/FOO/history').to route_to(no_layout: true, controller: 'api/pipelines', action: 'history', pipeline_name: 'FOO', :offset => '0')
      end

      it 'should not route to history action of pipelines controller for invalid pipeline name' do
        expect(:get => 'api/pipelines/fo$%#@6/history').to_not be_routable
      end
    end

  end

  describe "instance_by_counter" do
    describe "with_pipeline_name_contraint" do
      it 'should route to instance_by_counter action of pipelines controller having dots in pipeline name' do
        expect(:get => 'api/pipelines/some.thing/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: 'some.thing', pipeline_counter: '1')
      end

      it 'should route to instance_by_counter action of pipelines controller having hyphen in pipeline name' do
        expect(:get => 'api/pipelines/some-thing/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: 'some-thing', pipeline_counter: '1')
      end

      it 'should route to instance_by_counter action of pipelines controller having underscore in pipeline name' do
        expect(:get => 'api/pipelines/some_thing/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: 'some_thing', pipeline_counter: '1')
      end

      it 'should route to instance_by_counter action of pipelines controller having alphanumeric pipeline name' do
        expect(:get => 'api/pipelines/123foo/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: '123foo', pipeline_counter: '1')
      end

      it 'should route to instance_by_counter action of pipelines controller having capitalized pipeline name' do
        expect(:get => 'api/pipelines/FOO/instance/1').to route_to(no_layout: true, controller: 'api/pipelines', action: 'instance_by_counter', pipeline_name: 'FOO', pipeline_counter: '1')
      end

      it 'should not route to instance_by_counter action of pipelines controller for invalid pipeline name' do
        expect(:get => 'api/pipelines/fo$%#@6/instance/1').to_not be_routable
      end
    end

    describe "with_pipeline_counter_constraint" do
      it 'should not route to instance_by_counter action of pipelines controller for invalid pipeline counter' do
        expect(:get => 'api/pipelines/some.thing/instance/fo$%#@6/2').to_not be_routable
      end
    end
  end

  describe "status" do
    it "should route to status" do
      expect(:get => '/api/pipelines/up42/status').to route_to(:controller => "api/pipelines", :action => "status", :pipeline_name => 'up42', :no_layout => true)
    end

    describe "with_pipeline_name_contraint" do
      it 'should route to status action of pipelines controller having dots in pipeline name' do
        expect(:get => 'api/pipelines/some.thing/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: 'some.thing')
      end

      it 'should route to status action of pipelines controller having hyphen in pipeline name' do
        expect(:get => 'api/pipelines/some-thing/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: 'some-thing')
      end

      it 'should route to status action of pipelines controller having underscore in pipeline name' do
        expect(:get => 'api/pipelines/some_thing/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: 'some_thing')
      end

      it 'should route to status action of pipelines controller having alphanumeric pipeline name' do
        expect(:get => 'api/pipelines/123foo/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: '123foo')
      end

      it 'should route to status action of pipelines controller having capitalized pipeline name' do
        expect(:get => 'api/pipelines/FOO/status').to route_to(no_layout: true, controller: 'api/pipelines', action: 'status', pipeline_name: 'FOO')
      end

      it 'should not route to status action of pipelines controller for invalid pipeline name' do
        expect(:get => 'api/pipelines/fo$%#@6/status').to_not be_routable
      end
    end

  end

  describe "schedule" do

    describe "with_header" do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      end
      describe "with_pipeline_name_constraint" do
        it 'should route to schedule action of pipelines controller having dots in pipeline name' do
          expect(post: 'api/pipelines/some.thing/schedule').to route_to(no_layout: true, controller: 'api/pipelines', action: 'schedule', pipeline_name: 'some.thing')
        end

        it 'should route to schedule action of pipelines controller having hyphen in pipeline name' do
          expect(post: 'api/pipelines/some-thing/schedule').to route_to(no_layout: true, controller: 'api/pipelines', action: 'schedule', pipeline_name: 'some-thing')
        end

        it 'should route to schedule action of pipelines controller having underscore in pipeline name' do
          expect(post: 'api/pipelines/some_thing/schedule').to route_to(no_layout: true, controller: 'api/pipelines', action: 'schedule', pipeline_name: 'some_thing')
        end

        it 'should route to schedule action of pipelines controller having alphanumeric pipeline name' do
          expect(post: 'api/pipelines/123foo/schedule').to route_to(no_layout: true, controller: 'api/pipelines', action: 'schedule', pipeline_name: '123foo')
        end

        it 'should route to schedule action of pipelines controller having capitalized pipeline name' do
          expect(post: 'api/pipelines/FOO/schedule').to route_to(no_layout: true, controller: 'api/pipelines', action: 'schedule', pipeline_name: 'FOO')
        end

        it 'should not route to schedule action of pipelines controller for invalid pipeline name' do
          expect(post: 'api/pipelines/fo$%#@6/schedule').to_not be_routable
        end
      end
    end
    describe "without_header" do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
      end
      it 'should not resolve route to schedule' do
        expect(:post => "api/pipelines/foo.bar/schedule").to_not route_to(controller: "api/pipelines", pipeline_name: "foo.bar", action: "schedule", no_layout: true)
        expect(post: 'api/pipelines/foo.bar/schedule').to route_to(controller: 'application', action: 'unresolved', url: 'api/pipelines/foo.bar/schedule')
      end
    end
  end

  describe "pipeline_instance" do
    it "should resolve url to action" do
      expect(:get => "/api/pipelines/pipeline.com/10.xml?foo=bar").to route_to(:controller => 'api/pipelines', :action => 'pipeline_instance', :name => "pipeline.com", :id => "10", :format => "xml", :foo => "bar", :no_layout => true)
    end

    describe "with_pipeline_name_contraint" do
      it 'should route to pipeline_instance action of pipelines controller having dots in pipeline name' do
        expect(:get => 'api/pipelines/some.thing/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: 'some.thing', id: '1')
      end

      it 'should route to pipeline_instance action of pipelines controller having hyphen in pipeline name' do
        expect(:get => 'api/pipelines/some-thing/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: 'some-thing', id: '1')
      end

      it 'should route to pipeline_instance action of pipelines controller having underscore in pipeline name' do
        expect(:get => 'api/pipelines/some_thing/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: 'some_thing', id: '1')
      end

      it 'should route to pipeline_instance action of pipelines controller having alphanumeric pipeline name' do
        expect(:get => 'api/pipelines/123foo/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: '123foo', id: '1')
      end

      it 'should route to pipeline_instance action of pipelines controller having capitalized pipeline name' do
        expect(:get => 'api/pipelines/FOO/1.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipeline_instance', name: 'FOO', id: '1')
      end

      it 'should not route to pipeline_instance action of pipelines controller for invalid pipeline name' do
        expect(:get => 'api/pipelines/fo$%#@6/1.xml').to_not be_routable
      end
    end

  end

  describe "pipelines" do
    it 'should resolve route to pipelines action' do
      expect(get: 'api/pipelines.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'pipelines')
    end
  end

  describe "stage_feed" do

    it "should answer for /api/pipelines/foo/stages.xml" do
      expect(:get => '/api/pipelines/foo/stages.xml').to route_to(:controller => "api/pipelines", :action => "stage_feed", :format => "xml", :name => 'foo', :no_layout => true)
    end

    it "should return stages url with before and after params" do
      expected_routing_params = {:controller => "api/pipelines", :action => "stage_feed", :format => "xml", :name => 'cruise', :no_layout => true, :after => "2", :before => "bar"}
      expect(:get => api_pipeline_stage_feed_path(:after => 2, :before => "bar", :name => "cruise")).to route_to(expected_routing_params)
    end

    describe "with_pipeline_name_contraint" do
      it 'should route to stage_feed action of pipelines controller having dots in pipeline name' do
        expect(:get => 'api/pipelines/some.thing/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: 'some.thing')
      end

      it 'should route to stage_feed action of pipelines controller having hyphen in pipeline name' do
        expect(:get => 'api/pipelines/some-thing/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: 'some-thing')
      end

      it 'should route to stage_feed action of pipelines controller having underscore in pipeline name' do
        expect(:get => 'api/pipelines/some_thing/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: 'some_thing')
      end

      it 'should route to stage_feed action of pipelines controller having alphanumeric pipeline name' do
        expect(:get => 'api/pipelines/123foo/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: '123foo')
      end

      it 'should route to stage_feed action of pipelines controller having capitalized pipeline name' do
        expect(:get => 'api/pipelines/FOO/stages.xml').to route_to(no_layout: true, format: 'xml', controller: 'api/pipelines', action: 'stage_feed', name: 'FOO')
      end

      it 'should not route to stage_feed action of pipelines controller for invalid pipeline name' do
        expect(:get => 'api/pipelines/fo$%#@6/stages.xml').to_not be_routable
      end
    end

  end

  describe "releaseLock" do

    describe "with_header" do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      end
      describe "with_pipeline_name_constraint" do
        it 'should route to releaseLock action of pipelines controller having dots in pipeline name' do
          expect(post: 'api/pipelines/some.thing/releaseLock').to route_to(no_layout: true, controller: 'api/pipelines', action: 'releaseLock', pipeline_name: 'some.thing')
        end

        it 'should route to releaseLock action of pipelines controller having hyphen in pipeline name' do
          expect(post: 'api/pipelines/some-thing/releaseLock').to route_to(no_layout: true, controller: 'api/pipelines', action: 'releaseLock', pipeline_name: 'some-thing')
        end

        it 'should route to releaseLock action of pipelines controller having underscore in pipeline name' do
          expect(post: 'api/pipelines/some_thing/releaseLock').to route_to(no_layout: true, controller: 'api/pipelines', action: 'releaseLock', pipeline_name: 'some_thing')
        end

        it 'should route to releaseLock action of pipelines controller having alphanumeric pipeline name' do
          expect(post: 'api/pipelines/123foo/releaseLock').to route_to(no_layout: true, controller: 'api/pipelines', action: 'releaseLock', pipeline_name: '123foo')
        end

        it 'should route to releaseLock action of pipelines controller having capitalized pipeline name' do
          expect(post: 'api/pipelines/FOO/releaseLock').to route_to(no_layout: true, controller: 'api/pipelines', action: 'releaseLock', pipeline_name: 'FOO')
        end

        it 'should not route to releaseLock action of pipelines controller for invalid pipeline name' do
          expect(post: 'api/pipelines/fo$%#@6/releaseLock').to_not be_routable
        end
      end
    end

    describe "without_header" do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
      end
      it 'should not resolve route to releaseLock' do
        expect(:post => "api/pipelines/foo.bar/releaseLock").to_not route_to(controller: "api/pipelines", pipeline_name: "foo.bar", action: "releaseLock", no_layout: true)
        expect(post: 'api/pipelines/foo.bar/releaseLock').to route_to(controller: 'application', action: 'unresolved', url: 'api/pipelines/foo.bar/releaseLock')
      end
    end
  end

  describe "pause" do

    describe "with_header" do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      end
      describe "with_pipeline_name_constraint" do
        it 'should route to pause action of pipelines controller having dots in pipeline name' do
          expect(post: 'api/pipelines/some.thing/pause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'pause', pipeline_name: 'some.thing')
        end

        it 'should route to pause action of pipelines controller having hyphen in pipeline name' do
          expect(post: 'api/pipelines/some-thing/pause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'pause', pipeline_name: 'some-thing')
        end

        it 'should route to pause action of pipelines controller having underscore in pipeline name' do
          expect(post: 'api/pipelines/some_thing/pause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'pause', pipeline_name: 'some_thing')
        end

        it 'should route to pause action of pipelines controller having alphanumeric pipeline name' do
          expect(post: 'api/pipelines/123foo/pause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'pause', pipeline_name: '123foo')
        end

        it 'should route to pause action of pipelines controller having capitalized pipeline name' do
          expect(post: 'api/pipelines/FOO/pause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'pause', pipeline_name: 'FOO')
        end

        it 'should not route to pause action of pipelines controller for invalid pipeline name' do
          expect(post: 'api/pipelines/fo$%#@6/pause').to_not be_routable
        end
      end
    end
    describe "without_header" do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
      end
      it 'should not resolve route to pause' do
        expect(:post => "api/pipelines/foo.bar/pause").to_not route_to(controller: "api/pipelines", pipeline_name: "foo.bar", action: "pause", no_layout: true)
        expect(post: 'api/pipelines/foo.bar/pause').to route_to(controller: 'application', action: 'unresolved', url: 'api/pipelines/foo.bar/pause')
      end
    end
  end

  describe "unpause" do
    describe "with_header" do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      end
      describe "with_pipeline_name_constraint" do
        it 'should route to unpause action of pipelines controller having dots in pipeline name' do
          expect(post: 'api/pipelines/some.thing/unpause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'unpause', pipeline_name: 'some.thing')
        end

        it 'should route to unpause action of pipelines controller having hyphen in pipeline name' do
          expect(post: 'api/pipelines/some-thing/unpause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'unpause', pipeline_name: 'some-thing')
        end

        it 'should route to unpause action of pipelines controller having underscore in pipeline name' do
          expect(post: 'api/pipelines/some_thing/unpause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'unpause', pipeline_name: 'some_thing')
        end

        it 'should route to unpause action of pipelines controller having alphanumeric pipeline name' do
          expect(post: 'api/pipelines/123foo/unpause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'unpause', pipeline_name: '123foo')
        end

        it 'should route to unpause action of pipelines controller having capitalized pipeline name' do
          expect(post: 'api/pipelines/FOO/unpause').to route_to(no_layout: true, controller: 'api/pipelines', action: 'unpause', pipeline_name: 'FOO')
        end

        it 'should not route to unpause action of pipelines controller for invalid pipeline name' do
          expect(post: 'api/pipelines/fo$%#@6/unpause').to_not be_routable
        end
      end
    end
    describe "without_header" do
      before :each do
        allow_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
      end
      it 'should not resolve route to unpause' do
        expect(:post => "api/pipelines/foo.bar/unpause").to_not route_to(controller: "api/pipelines", pipeline_name: "foo.bar", action: "unpause", no_layout: true)
        expect(post: 'api/pipelines/foo.bar/unpause').to route_to(controller: 'application', action: 'unresolved', url: 'api/pipelines/foo.bar/unpause')
      end
    end
  end

  def schedule_options(specified_revisions, variables, secure_variables = {})
    ScheduleOptions.new(HashMap.new(specified_revisions), LinkedHashMap.new(variables), HashMap.new(secure_variables))
  end

end
