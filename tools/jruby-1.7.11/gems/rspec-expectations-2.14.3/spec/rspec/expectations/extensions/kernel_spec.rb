require 'spec_helper'

describe Object, "#should" do
  before(:each) do
    @target = "target"
    @matcher = double("matcher")
    @matcher.stub(:matches?).and_return(true)
    @matcher.stub(:failure_message_for_should)
  end

  it "accepts and interacts with a matcher" do
    @matcher.should_receive(:matches?).with(@target).and_return(true)
    expect(@target).to @matcher
  end

  it "asks for a failure_message_for_should when matches? returns false" do
    @matcher.should_receive(:matches?).with(@target).and_return(false)
    @matcher.should_receive(:failure_message_for_should).and_return("the failure message")
    expect {
      expect(@target).to @matcher
    }.to fail_with("the failure message")
  end

  context "on interpretters that have BasicObject", :if => defined?(BasicObject) do
    let(:proxy_class) do
      Class.new(BasicObject) do
        def initialize(target)
          @target = target
        end

        def proxied?
          true
        end

        def method_missing(name, *args)
          @target.send(name, *args)
        end
      end
    end

    it 'works properly on BasicObject-subclassed proxy objects' do
      expect(proxy_class.new(Object.new)).to be_proxied
    end
  end
end

describe Object, "#should_not" do
  before(:each) do
    @target = "target"
    @matcher = double("matcher")
  end

  it "accepts and interacts with a matcher" do
    @matcher.should_receive(:matches?).with(@target).and_return(false)
    @matcher.stub(:failure_message_for_should_not)

    expect(@target).not_to @matcher
  end

  it "asks for a failure_message_for_should_not when matches? returns true" do
    @matcher.should_receive(:matches?).with(@target).and_return(true)
    @matcher.should_receive(:failure_message_for_should_not).and_return("the failure message for should not")
    expect {
      expect(@target).not_to @matcher
    }.to fail_with("the failure message for should not")
  end
end
