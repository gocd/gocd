require File.dirname(__FILE__) + "/../../../spec_helper"

module Spec
  module Runner
    module Formatter
      describe BaseFormatter do
        subject { BaseFormatter.new(nil,nil) }
        it {should respond_to(:start                ).with(1).argument }
        it {should respond_to(:example_group_started).with(1).argument }
        it {should respond_to(:example_started      ).with(1).argument }
        # -3 indicates that one of the arguments is optional, with a default
        # value assigned. This is due to deprecated_pending_location. Once
        # that is removed, this should be changed to 2.
        it {should respond_to(:example_pending      ).with(-3).arguments}
        it {should respond_to(:example_passed       ).with(1).argument }
        it {should respond_to(:example_failed       ).with(3).arguments}
        it {should respond_to(:start_dump           ).with(0).arguments}
        it {should respond_to(:dump_failure         ).with(2).arguments}
        it {should respond_to(:dump_summary         ).with(4).arguments}
        it {should respond_to(:dump_pending         ).with(0).arguments}
        it {should respond_to(:close                ).with(0).arguments}
        
        it "deprecates add_example_group" do
          Spec.should_receive(:deprecate)
          subject.add_example_group(stub('example group'))
        end
      end
    end
  end
end
