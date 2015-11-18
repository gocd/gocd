require File.dirname(__FILE__) + '/spec_helper'

describe YARD::Handlers::Processor do
  before do
    @proc = Handlers::Processor.new(OpenStruct.new(:parser_type => :ruby))
  end

  it "should start with public visibility" do
    @proc.visibility.should == :public
  end

  it "should start in instance scope" do
    @proc.scope.should == :instance
  end

  it "should start in root namespace" do
    @proc.namespace.should == Registry.root
  end

  it "should have a globals structure" do
    @proc.globals.should be_a(OpenStruct)
  end

  it 'should ignore HandlerAborted exceptions (but print debug info)' do
    class AbortHandlerProcessor < YARD::Handlers::Ruby::Base
      process { abort! }
    end
    stmt = OpenStruct.new(:line => 1, :show => 'SOURCE')
    @proc.stub!(:find_handlers).and_return([AbortHandlerProcessor])
    log.should_receive(:debug).with(/AbortHandlerProcessor cancelled from/)
    log.should_receive(:debug).with("\tin file '(stdin)':1:\n\nSOURCE\n")
    @proc.process([stmt])
  end
end
