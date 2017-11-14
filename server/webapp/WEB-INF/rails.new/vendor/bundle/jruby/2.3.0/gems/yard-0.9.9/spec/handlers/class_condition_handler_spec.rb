# frozen_string_literal: true
require File.dirname(__FILE__) + '/spec_helper'

RSpec.describe "YARD::Handlers::Ruby::#{LEGACY_PARSER ? "Legacy::" : ""}ClassConditionHandler" do
  before(:all) { parse_file :class_condition_handler_001, __FILE__ }

  def verify_method(*names)
    names.each {|name| expect(Registry.at("A##{name}")).not_to be nil }
    names.each {|name| expect(Registry.at("A##{name}not")).to be nil }
  end

  def no_undoc_error(code)
    expect { StubbedSourceParser.parse_string(code) }.not_to raise_error
  end

  it "parses all unless blocks for complex conditions" do
    verify_method :g
  end

  it "does not parse conditionals inside methods" do
    verify_method :h
  end

  it "only parses then block if condition is literal value `true`" do
    verify_method :p
  end

  it "only parses then block if condition is literal integer != 0" do
    verify_method :o
  end

  it "inverts block to parse for literal condition if it's an unless block" do
    verify_method :e
  end

  it "handles conditions such as 'defined? VALUE'" do
    verify_method :j, :k
  end

  it "parses all if/elsif blocks for complex conditions" do
    verify_method :a, :b, :c, :d
  end

  it "parses else block if condition is literal value `false`" do
    verify_method :q
  end

  it "only parses else block if condition is literal integer == 0" do
    verify_method :n
  end

  it "maintains visibility and scope state inside condition" do
    expect(Registry.at('A#m').visibility).to eq :private
    expect(Registry.at('A#mnot').visibility).to eq :private
  end

  it "does not fail on complex conditions" do
    expect(log).not_to receive(:warn)
    expect(log).not_to receive(:error)
    no_undoc_error "if defined?(A) && defined?(B); puts 'hi' end"
    no_undoc_error(<<-eof)
      (<<-TEST) unless defined?(ABCD_MODEL_TEST)
        'String'
      TEST
    eof
    no_undoc_error "if caller.none? { |l| l =~ %r{lib/rails/generators\\.rb:(\\d+):in `lookup!'$} }; end"
  end
end
