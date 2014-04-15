require File.dirname(__FILE__) + '/../../spec_helper'
require File.dirname(__FILE__) + '/../../ruby_forker'

describe "The bin/spec script" do
  include RubyForker
  
  it "should have no warnings" do
    pending "Hangs on JRuby" if RUBY_PLATFORM =~ /java/
    location = "#{File.dirname(__FILE__)}/../../../bin/spec"

    output = ruby "-w #{location} --help 2>&1"
    output.should_not =~ /warning/n
  end
  
  it "should show the help w/ no args" do
    pending "Hangs on JRuby" if RUBY_PLATFORM =~ /java/
    location = "#{File.dirname(__FILE__)}/../../../bin/spec"

    output = ruby "-w #{location} 2>&1"
    output.should =~ /^Usage: spec/
  end
end
