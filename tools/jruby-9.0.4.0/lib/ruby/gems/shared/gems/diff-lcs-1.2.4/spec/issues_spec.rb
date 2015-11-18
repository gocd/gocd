# -*- ruby encoding: utf-8 -*-

require 'spec_helper'

describe "Diff::LCS Issues" do
  include Diff::LCS::SpecHelper::Matchers

  it "should not fail to provide a simple patchset (issue 1)" do
    s1, s2 = *%W(aX bXaX)
    correct_forward_diff = [
      [ [ '+', 0, 'b' ],
        [ '+', 1, 'X' ] ],
    ]

    diff_s1_s2 = Diff::LCS.diff(s1, s2)
    change_diff(correct_forward_diff).should == diff_s1_s2
    expect do
      Diff::LCS.patch(s1, diff_s1_s2).should == s2
    end.to_not raise_error(RuntimeError, /provided patchset/)
    expect do
      Diff::LCS.patch(s2, diff_s1_s2).should == s1
    end.to_not raise_error(RuntimeError, /provided patchset/)
  end
end
