require File.dirname(__FILE__) + '/../../spec_helper'

module Spec
  module Runner
    describe ExampleGroupRunner do
      before(:each) do
        err = StringIO.new('')
        out = StringIO.new('')
        @options = Options.new(err, out)
        @runner = Spec::Runner::ExampleGroupRunner.new(@options)
      end

      after(:each) do
        Spec::Expectations.differ = nil
      end

      describe "#load_files" do
        it "should load UTF-8 encoded files" do
          file = File.expand_path(File.dirname(__FILE__) + "/resources/utf8_encoded.rb")
          @options.files << file
          @runner.load_files(@options.files_to_load).should == @options.files_to_load
        end
      end
    end
  end
end
