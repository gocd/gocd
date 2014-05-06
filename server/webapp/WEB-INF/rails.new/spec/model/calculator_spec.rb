require File.join(File.dirname(__FILE__), "..", "spec_helper")

describe "Calculator" do
  it "should add numbers correctly" do
    calc = Calculator.new
    calc.sum(1,3).should == 4
  end
end