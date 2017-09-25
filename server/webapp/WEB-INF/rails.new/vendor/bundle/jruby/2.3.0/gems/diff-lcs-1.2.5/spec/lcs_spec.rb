# -*- ruby encoding: utf-8 -*-

require 'spec_helper'

describe "Diff::LCS::Internals.lcs" do
  include Diff::LCS::SpecHelper::Matchers

  it "should return a meaningful LCS array with (seq1, seq2)" do
    res = Diff::LCS::Internals.lcs(seq1, seq2)
    # The result of the LCS (less the +nil+ values) must be as long as the
    # correct result.
    res.compact.size.should == correct_lcs.size
    res.should correctly_map_sequence(seq1).to_other_sequence(seq2)

    # Compact these transformations and they should be the correct LCS.
    x_seq1 = (0...res.size).map { |ix| res[ix] ? seq1[ix] : nil }.compact
    x_seq2 = (0...res.size).map { |ix| res[ix] ? seq2[res[ix]] : nil }.compact

    x_seq1.should == correct_lcs
    x_seq2.should == correct_lcs
  end

  it "should return all indexes with (hello, hello)" do
    Diff::LCS::Internals.lcs(hello, hello).should == (0...hello.size).to_a
  end

  it "should return all indexes with (hello_ary, hello_ary)" do
    Diff::LCS::Internals.lcs(hello_ary, hello_ary).should == (0...hello_ary.size).to_a
  end
end

describe "Diff::LCS.LCS" do
  include Diff::LCS::SpecHelper::Matchers

  it "should return the correct compacted values from Diff::LCS.LCS" do
    res = Diff::LCS.LCS(seq1, seq2)
    res.should == correct_lcs
    res.compact.should == res
  end

  it "should be transitive" do
    res = Diff::LCS.LCS(seq2, seq1)
    res.should == correct_lcs
    res.compact.should == res
  end

  it "should return %W(h e l l o) with (hello, hello)" do
    Diff::LCS.LCS(hello, hello).should == hello.split(//)
  end

  it "should return hello_ary with (hello_ary, hello_ary)" do
    Diff::LCS.LCS(hello_ary, hello_ary).should == hello_ary
  end
end
