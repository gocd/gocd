require File.dirname(__FILE__) + "/autotest_helper"

describe "failed_results_re" do
  it "should match a failure" do
    re = Autotest::Rspec.new.failed_results_re
    re =~ "1)\n'this example' FAILED\nreason\n/path.rb:37:\n\n"
    $1.should == "this example"
    $2.should == "reason\n/path.rb:37:"
  end

  it "should match a failure when matcher outputs multiple lines" do
    re = Autotest::Rspec.new.failed_results_re
    re =~ "1)\n'other example' FAILED\n\nreason line 1\nreason line 2\n\n(additional info)\n/path.rb:37:\n\n"
    $1.should == "other example"
    $2.should == "reason line 1\nreason line 2\n\n(additional info)\n/path.rb:37:"
  end

  it "should match an Error" do
    re = Autotest::Rspec.new.failed_results_re
    re =~ "1)\nRuntimeError in 'this example'\nreason\n/path.rb:37:\n\n"
    $1.should == "this example"
    $2.should == "reason\n/path.rb:37:"
  end

  it "should match an Error that doesn't end in Error" do
    re = Autotest::Rspec.new.failed_results_re
    re =~ "1)\nInvalidArgument in 'this example'\nreason\n/path.rb:37:\n\n"
    $1.should == "this example"
    $2.should == "reason\n/path.rb:37:"
  end
end