require File.dirname(__FILE__) + '/spec_helper'

# Run "spec implicit_docstrings_example.rb --format specdoc" to see the output of this file

describe "Examples with no docstrings generate their own:" do

  specify { 3.should be < 5 }

  specify { ["a"].should include("a") }

  specify { [1,2,3].should respond_to(:size) }

end

describe 1 do
  it { should == 1 }
  it { should be < 2}
end
