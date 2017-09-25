require 'spec_helper'

describe "An object where respond_to? is true and does not have method" do
  # When should_receive(message) is sent to any object, the Proxy sends
  # respond_to?(message) to that object to see if the method should be proxied.
  #
  # If respond_to? itself is proxied, then when the Proxy sends respond_to?
  # to the object, the proxy is invoked and responds yes (if so set in the spec).
  # When the object does NOT actually respond to `message`, an exception is thrown
  # when trying to proxy it.
  #
  # The fix was to keep track of whether `respond_to?` had been proxied and, if
  # so, call the munged copy of `respond_to?` on the object.

  it "does not raise an exception for Object" do
    obj = Object.new
    obj.should_receive(:respond_to?).with(:foobar).and_return(true)
    obj.should_receive(:foobar).and_return(:baz)
    expect(obj.respond_to?(:foobar)).to be_true
    expect(obj.foobar).to eq :baz
  end

  it "does not raise an exception for mock" do
    obj = double("obj")
    obj.should_receive(:respond_to?).with(:foobar).and_return(true)
    obj.should_receive(:foobar).and_return(:baz)
    expect(obj.respond_to?(:foobar)).to be_true
    expect(obj.foobar).to eq :baz
  end

end
