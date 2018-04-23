require 'spec_helper'
require 'nokogiri'
require 'ostruct'

describe Jasmine::Page do
  describe "#render" do
    subject { Nokogiri::HTML(page.render) }
    let(:fake_config) do
      OpenStruct.new(:js_files => ["file1.js", "file2.js"], :css_files => ["file1.css", "file2.css"])
    end
    let(:context) { fake_config }
    let(:page) { Jasmine::Page.new(context) }
    it "should render javascript files in the correct order" do
      js_files = subject.css("script")
      expect(js_files.map { |file| file["src"] }.compact).to eq ["file1.js", "file2.js"]
    end

    it "should render css files in the correct order" do
      css_files = subject.css("link[type='text/css']")
      expect(css_files.map { |file| file["href"] }.compact).to eq ["file1.css", "file2.css"]
    end
  end
end
