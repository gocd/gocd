require File.dirname(__FILE__) + '/../spec_helper'

describe YARD::CLI::Display do
  before do
    Registry.stub(:load)
    @object = CodeObjects::ClassObject.new(:root, :Foo)
    @object.docstring = 'Foo bar'
  end

  it "displays an object" do
    YARD::CLI::Display.run('-f', 'text', 'Foo')
    log.io.string.strip.should eq(@object.format.strip)
  end

  it "wraps output with -l (defaulting to layout)" do
    YARD::CLI::Display.run('-l', '-f', 'html', 'Foo')
    formatted_output = @object.format(:format => :html).strip
    actual_output = log.io.string.strip
    actual_output.should_not eq(formatted_output)
    actual_output.should include(formatted_output)
  end

  it "wraps output with --layout onefile" do
    YARD::CLI::Display.run('--layout', 'onefile', '-f', 'html', 'Foo')
    formatted_output = @object.format(:format => :html).strip
    actual_output = log.io.string.strip
    actual_output.should_not eq(formatted_output)
    actual_output.should include(formatted_output)
  end
end
