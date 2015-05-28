require File.dirname(__FILE__) + '/../spec_helper'

describe YARD::CLI::Graph do
  it "should serialize output" do
    Registry.should_receive(:load).at_least(1).times
    subject.stub(:yardopts) { [] }
    subject.options.serializer.should_receive(:serialize).once
    subject.run
  end

  it 'should read yardoc file from .yardopts' do
    subject.stub(:yardopts) { %w(--db /path/to/db) }
    subject.options.serializer.should_receive(:serialize).once
    subject.run
    Registry.yardoc_file.should == '/path/to/db'
  end
end
