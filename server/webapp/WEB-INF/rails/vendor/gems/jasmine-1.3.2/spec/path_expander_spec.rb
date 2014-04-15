require 'spec_helper'

describe Jasmine::PathExpander do
  it "returns absolute paths" do
    dir_glob = lambda do |pattern|
      case pattern
      when 'some_base/src1*'
        ['some_base/src1.js', 'some_base/src15.js']
      when 'some_base/src2*'
        ['some_base/src2.js']
      else
        raise "Unexpected pattern received: #{pattern}"
      end
    end

    expanded_files = Jasmine::PathExpander.expand(
      'some_base',
      ['src1*', 'src2*'],
      dir_glob
    )

    expanded_files.should == [
      File.join('some_base', 'src1.js'),
      File.join('some_base', 'src15.js'),
      File.join('some_base', 'src2.js')
    ]
  end

  it "uniqs files" do
    dir_glob = lambda do |pattern|
      case pattern
      when 'some_base/src1*'
        ['some_base/src1.js', 'some_base/src15.js', 'some_base/src1.js']
      when 'some_base/src2*'
        ['some_base/src2.js']
      else
        raise "Unexpected pattern received: #{pattern}"
      end
    end

    expanded_files = Jasmine::PathExpander.expand(
      'some_base',
      ['src1*', 'src2*'],
      dir_glob
    )

    expanded_files.should == [
      File.join('some_base', 'src1.js'),
      File.join('some_base', 'src15.js'),
      File.join('some_base', 'src2.js')
    ]
  end

  it "supports negation of passed patterns" do
    dir_glob = lambda do |pattern|
      case pattern
      when 'some_base/src1*'
        ['some_base/src1.js', 'some_base/src15.js']
      when 'some_base/src1.js'
        ['some_base/src1.js']
      when 'some_base/src2*'
        ['some_base/src2.js']
      else
        raise "Unexpected pattern received: #{pattern}"
      end
    end

    expanded_files = Jasmine::PathExpander.expand(
      'some_base',
      ['src1*', '!src1.js', 'src2*'],
      dir_glob
    )

    expanded_files.should == [
      File.join('some_base', 'src15.js'),
      File.join('some_base', 'src2.js')
    ]
  end

  it "passes through files that are not found by the globber and are not negations and not globs" do
    #this is designed to support asset pipeline files that aren't found.
    dir_glob = lambda do |pattern|
      []
    end

    expanded_files = Jasmine::PathExpander.expand(
      'some_base',
      ['src1*', '!src1.js', 'src2.js'],
      dir_glob
    )

    expanded_files.should == [
      File.join('some_base', 'src2.js')
    ]
  end
end
