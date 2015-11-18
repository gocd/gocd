require 'spec_helper'
require 'rack/jasmine/runner'

describe Rack::Jasmine::Runner do
  describe "#call" do
    let(:content) { "some content" }
    let(:page) { double(Jasmine::Page, :render => content)}
    let(:runner) { Rack::Jasmine::Runner.new(page)}
    subject { runner.call("PATH_INFO" => path) }
    context "PATH_INFO is /" do
      let(:expected_headers) { {"Content-Type" => "text/html"} }
      let(:path) { "/" }
      it "should return a response with the passed content" do
        subject.should == [200, expected_headers, [content]]
      end
    end
    context "PATH_INFO is not /" do
      let(:path) { "/some_foo" }
      let(:expected_headers) { {"Content-Type" => "text/plain", "X-Cascade" => "pass"} }
      it "should return a 404" do
        subject.should == [404, expected_headers, ["File not found: #{path}\n"]]
      end
    end
  end
end
