require File.dirname(__FILE__) + '/../../../spec_helper'

module Spec
  module Runner
    describe Configuration do

      def config
        @config ||= Configuration.new
      end

      describe "#use_transactional_fixtures" do
        it "should return ActiveSupport::TestCase.use_transactional_fixtures" do
          config.use_transactional_fixtures.should == ActiveSupport::TestCase.use_transactional_fixtures
        end

        it "should set ActiveSupport::TestCase.use_transactional_fixtures to false" do
          ActiveSupport::TestCase.should_receive(:use_transactional_fixtures=).with(false)
          config.use_transactional_fixtures = false
        end

        it "should set ActiveSupport::TestCase.use_transactional_fixtures to true" do
          ActiveSupport::TestCase.should_receive(:use_transactional_fixtures=).with(true)
          config.use_transactional_fixtures = true
        end
      end

      describe "#use_instantiated_fixtures" do
        it "should return ActiveSupport::TestCase.use_transactional_fixtures" do
          config.use_instantiated_fixtures.should == ActiveSupport::TestCase.use_instantiated_fixtures
        end

        it "should set ActiveSupport::TestCase.use_instantiated_fixtures to false" do
          ActiveSupport::TestCase.should_receive(:use_instantiated_fixtures=).with(false)
          config.use_instantiated_fixtures = false
        end

        it "should set ActiveSupport::TestCase.use_instantiated_fixtures to true" do
          ActiveSupport::TestCase.should_receive(:use_instantiated_fixtures=).with(true)
          config.use_instantiated_fixtures = true
        end
      end

      describe "#fixture_path" do
        it "should default to RAILS_ROOT + '/spec/fixtures'" do
          config.fixture_path.should == RAILS_ROOT + '/spec/fixtures'
          ActiveSupport::TestCase.fixture_path.should == RAILS_ROOT + '/spec/fixtures'
        end

        it "should set fixture_path" do
          config.fixture_path = "/new/path"
          config.fixture_path.should == "/new/path"
          ActiveSupport::TestCase.fixture_path.should == "/new/path"
        end
      end

      describe "#global_fixtures" do
        it "should set fixtures on TestCase" do
          ActiveSupport::TestCase.should_receive(:fixtures).with(:blah)
          config.global_fixtures = [:blah]
        end
      end
      
    end
  end
end
