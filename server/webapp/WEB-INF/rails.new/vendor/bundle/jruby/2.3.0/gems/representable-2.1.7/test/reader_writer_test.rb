require 'test_helper'

class ReaderWriterTest < BaseTest
  representer! do
    property :name,
      :writer => lambda { |doc, args| doc["title"] = "#{args[:nr]}) #{name}" },
      :reader => lambda { |doc, args| self.name = doc["title"].split(") ").last }
  end

  subject { OpenStruct.new(:name => "Disorder And Disarray").extend(representer) }

  it "uses :writer when rendering" do
    subject.to_hash(:nr => 14).must_equal({"title" => "14) Disorder And Disarray"})
  end

  it "uses :reader when parsing" do
    subject.from_hash({"title" => "15) The Wars End"}).name.must_equal "The Wars End"
  end
end