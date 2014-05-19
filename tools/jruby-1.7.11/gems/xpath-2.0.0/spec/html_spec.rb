require 'spec_helper'
require 'nokogiri'

describe XPath::HTML do
  let(:template) { 'form' }
  let(:template_path) { File.read(File.expand_path("fixtures/#{template}.html", File.dirname(__FILE__))) }
  let(:doc) { Nokogiri::HTML(template_path) }

  def get(*args)
    all(*args).first
  end

  def all(*args)
    type = example.metadata[:type]
    doc.xpath(XPath::HTML.send(subject, *args).to_xpath(type)).map { |node| node[:data] }
  end

  describe '#link' do
    subject { :link }

    it("finds links by id")                                { get('some-id').should == 'link-id' }
    it("finds links by content")                           { get('An awesome link').should == 'link-text' }
    it("finds links by content regardless of whitespace")  { get('My whitespaced link').should == 'link-whitespace' }
    it("finds links with child tags by content")           { get('An emphatic link').should == 'link-children' }
    it("finds links by the content of their child tags")   { get('emphatic').should == 'link-children' }
    it("finds links by approximate content")               { get('awesome').should == 'link-text' }
    it("finds links by title")                             { get('My title').should == 'link-title' }
    it("finds links by approximate title")                 { get('title').should == 'link-title' }
    it("finds links by image's alt attribute")             { get('Alt link').should == 'link-img' }
    it("finds links by image's approximate alt attribute") { get('Alt').should == 'link-img' }
    it("does not find links without href attriutes")       { get('Wrong Link').should be_nil }
    it("casts to string")                                  { get(:'some-id').should == 'link-id' }

    context "with exact match", :type => :exact do
      it("finds links by content")                                   { get('An awesome link').should == 'link-text' }
      it("does not find links by approximate content")               { get('awesome').should be_nil }
      it("finds links by title")                                     { get('My title').should == 'link-title' }
      it("does not find links by approximate title")                 { get('title').should be_nil }
      it("finds links by image's alt attribute")                     { get('Alt link').should == 'link-img' }
      it("does not find links by image's approximate alt attribute") { get('Alt').should be_nil }
    end
  end

  describe '#button' do
    subject { :button }

    context "with submit type" do
      it("finds buttons by id")                { get('submit-with-id').should == 'id-submit' }
      it("finds buttons by value")             { get('submit-with-value').should == 'value-submit' }
      it("finds buttons by approximate value") { get('mit-with-val').should == 'value-submit' }
      it("finds buttons by title")             { get('My submit title').should == 'title-submit' }
      it("finds buttons by approximate title") { get('submit title').should == 'title-submit' }

      context "with exact match", :type => :exact do
        it("finds buttons by value")                     { get('submit-with-value').should == 'value-submit' }
        it("does not find buttons by approximate value") { get('mit-with-val').should be_nil }
        it("finds buttons by title")                     { get('My submit title').should == 'title-submit' }
        it("does not find buttons by approximate title") { get('submit title').should be_nil }
      end
    end

    context "with reset type" do
      it("finds buttons by id")                { get('reset-with-id').should == 'id-reset' }
      it("finds buttons by value")             { get('reset-with-value').should == 'value-reset' }
      it("finds buttons by approximate value") { get('set-with-val').should == 'value-reset' }
      it("finds buttons by title")             { get('My reset title').should == 'title-reset' }
      it("finds buttons by approximate title") { get('reset title').should == 'title-reset' }

      context "with exact match", :type => :exact do
        it("finds buttons by value")                     { get('reset-with-value').should == 'value-reset' }
        it("does not find buttons by approximate value") { get('set-with-val').should be_nil }
        it("finds buttons by title")                     { get('My reset title').should == 'title-reset' }
        it("does not find buttons by approximate title") { get('reset title').should be_nil }
      end
    end

    context "with button type" do
      it("finds buttons by id")                { get('button-with-id').should == 'id-button' }
      it("finds buttons by value")             { get('button-with-value').should == 'value-button' }
      it("finds buttons by approximate value") { get('ton-with-val').should == 'value-button' }
      it("finds buttons by title")             { get('My button title').should == 'title-button' }
      it("finds buttons by approximate title") { get('button title').should == 'title-button' }

      context "with exact match", :type => :exact do
        it("finds buttons by value")                     { get('button-with-value').should == 'value-button' }
        it("does not find buttons by approximate value") { get('ton-with-val').should be_nil }
        it("finds buttons by title")                     { get('My button title').should == 'title-button' }
        it("does not find buttons by approximate title") { get('button title').should be_nil }
      end
    end

    context "with image type" do
      it("finds buttons by id")                        { get('imgbut-with-id').should == 'id-imgbut' }
      it("finds buttons by value")                     { get('imgbut-with-value').should == 'value-imgbut' }
      it("finds buttons by approximate value")         { get('gbut-with-val').should == 'value-imgbut' }
      it("finds buttons by alt attribute")             { get('imgbut-with-alt').should == 'alt-imgbut' }
      it("finds buttons by approximate alt attribute") { get('mgbut-with-al').should == 'alt-imgbut' }
      it("finds buttons by title")                     { get('My imgbut title').should == 'title-imgbut' }
      it("finds buttons by approximate title")         { get('imgbut title').should == 'title-imgbut' }

      context "with exact match", :type => :exact do
        it("finds buttons by value")                             { get('imgbut-with-value').should == 'value-imgbut' }
        it("does not find buttons by approximate value")         { get('gbut-with-val').should be_nil }
        it("finds buttons by alt attribute")                     { get('imgbut-with-alt').should == 'alt-imgbut' }
        it("does not find buttons by approximate alt attribute") { get('mgbut-with-al').should be_nil }
        it("finds buttons by title")                             { get('My imgbut title').should == 'title-imgbut' }
        it("does not find buttons by approximate title")         { get('imgbut title').should be_nil }
      end
    end

    context "with button tag" do
      it("finds buttons by id")                       { get('btag-with-id').should == 'id-btag' }
      it("finds buttons by value")                    { get('btag-with-value').should == 'value-btag' }
      it("finds buttons by approximate value")        { get('tag-with-val').should == 'value-btag' }
      it("finds buttons by text")                     { get('btag-with-text').should == 'text-btag' }
      it("finds buttons by text ignoring whitespace") { get('My whitespaced button').should == 'btag-with-whitespace' }
      it("finds buttons by approximate text ")        { get('tag-with-tex').should == 'text-btag' }
      it("finds buttons with child tags by text")     { get('An emphatic button').should == 'btag-with-children' }
      it("finds buttons by text of their children")   { get('emphatic').should == 'btag-with-children' }
      it("finds buttons by title")                    { get('My btag title').should == 'title-btag' }
      it("finds buttons by approximate title")        { get('btag title').should == 'title-btag' }

      context "with exact match", :type => :exact do
        it("finds buttons by value")                     { get('btag-with-value').should == 'value-btag' }
        it("does not find buttons by approximate value") { get('tag-with-val').should be_nil }
        it("finds buttons by text")                      { get('btag-with-text').should == 'text-btag' }
        it("does not find buttons by approximate text ") { get('tag-with-tex').should be_nil }
        it("finds buttons by title")                     { get('My btag title').should == 'title-btag' }
        it("does not find buttons by approximate title") { get('btag title').should be_nil }
      end
    end

    context "with unkown type" do
      it("does not find the button") { get('schmoo button').should be_nil }
    end

    it("casts to string") { get(:'tag-with-tex').should == 'text-btag' }
  end

  describe '#fieldset' do
    subject { :fieldset }

    it("finds fieldsets by id")                  { get('some-fieldset-id').should == 'fieldset-id' }
    it("finds fieldsets by legend")              { get('Some Legend').should == 'fieldset-legend' }
    it("finds fieldsets by legend child tags")   { get('Span Legend').should == 'fieldset-legend-span' }
    it("accepts approximate legends")            { get('Legend').should == 'fieldset-legend' }
    it("finds nested fieldsets by legend")       { get('Inner legend').should == 'fieldset-inner' }
    it("casts to string")                        { get(:'Inner legend').should == 'fieldset-inner' }

    context "with exact match", :type => :exact do
      it("finds fieldsets by legend")            { get('Some Legend').should == 'fieldset-legend' }
      it("does not find by approximate legends") { get('Legend').should be_nil }
    end
  end

  describe '#field' do
    subject { :field }

    context "by id" do
      it("finds inputs with no type")       { get('input-with-id').should == 'input-with-id-data' }
      it("finds inputs with text type")     { get('input-text-with-id').should == 'input-text-with-id-data' }
      it("finds inputs with password type") { get('input-password-with-id').should == 'input-password-with-id-data' }
      it("finds inputs with custom type")   { get('input-custom-with-id').should == 'input-custom-with-id-data' }
      it("finds textareas")                 { get('textarea-with-id').should == 'textarea-with-id-data' }
      it("finds select boxes")              { get('select-with-id').should == 'select-with-id-data' }
      it("does not find submit buttons")    { get('input-submit-with-id').should be_nil }
      it("does not find image buttons")     { get('input-image-with-id').should be_nil }
      it("does not find hidden fields")     { get('input-hidden-with-id').should be_nil }
    end

    context "by name" do
      it("finds inputs with no type")       { get('input-with-name').should == 'input-with-name-data' }
      it("finds inputs with text type")     { get('input-text-with-name').should == 'input-text-with-name-data' }
      it("finds inputs with password type") { get('input-password-with-name').should == 'input-password-with-name-data' }
      it("finds inputs with custom type")   { get('input-custom-with-name').should == 'input-custom-with-name-data' }
      it("finds textareas")                 { get('textarea-with-name').should == 'textarea-with-name-data' }
      it("finds select boxes")              { get('select-with-name').should == 'select-with-name-data' }
      it("does not find submit buttons")    { get('input-submit-with-name').should be_nil }
      it("does not find image buttons")     { get('input-image-with-name').should be_nil }
      it("does not find hidden fields")     { get('input-hidden-with-name').should be_nil }
    end

    context "by placeholder" do
      it("finds inputs with no type")       { get('input-with-placeholder').should == 'input-with-placeholder-data' }
      it("finds inputs with text type")     { get('input-text-with-placeholder').should == 'input-text-with-placeholder-data' }
      it("finds inputs with password type") { get('input-password-with-placeholder').should == 'input-password-with-placeholder-data' }
      it("finds inputs with custom type")   { get('input-custom-with-placeholder').should == 'input-custom-with-placeholder-data' }
      it("finds textareas")                 { get('textarea-with-placeholder').should == 'textarea-with-placeholder-data' }
      it("does not find hidden fields")     { get('input-hidden-with-placeholder').should be_nil }
    end

    context "by referenced label" do
      it("finds inputs with no type")       { get('Input with label').should == 'input-with-label-data' }
      it("finds inputs with text type")     { get('Input text with label').should == 'input-text-with-label-data' }
      it("finds inputs with password type") { get('Input password with label').should == 'input-password-with-label-data' }
      it("finds inputs with custom type")   { get('Input custom with label').should == 'input-custom-with-label-data' }
      it("finds textareas")                 { get('Textarea with label').should == 'textarea-with-label-data' }
      it("finds select boxes")              { get('Select with label').should == 'select-with-label-data' }
      it("does not find submit buttons")    { get('Input submit with label').should be_nil }
      it("does not find image buttons")     { get('Input image with label').should be_nil }
      it("does not find hidden fields")     { get('Input hidden with label').should be_nil }
    end

    context "by parent label" do
      it("finds inputs with no type")       { get('Input with parent label').should == 'input-with-parent-label-data' }
      it("finds inputs with text type")     { get('Input text with parent label').should == 'input-text-with-parent-label-data' }
      it("finds inputs with password type") { get('Input password with parent label').should == 'input-password-with-parent-label-data' }
      it("finds inputs with custom type")   { get('Input custom with parent label').should == 'input-custom-with-parent-label-data' }
      it("finds textareas")                 { get('Textarea with parent label').should == 'textarea-with-parent-label-data' }
      it("finds select boxes")              { get('Select with parent label').should == 'select-with-parent-label-data' }
      it("does not find submit buttons")    { get('Input submit with parent label').should be_nil }
      it("does not find image buttons")     { get('Input image with parent label').should be_nil }
      it("does not find hidden fields")     { get('Input hidden with parent label').should be_nil }
    end

    it("casts to string") { get(:'select-with-id').should == 'select-with-id-data' }
  end

  describe '#fillable_field' do
    subject{ :fillable_field }
    context "by parent label" do
      it("finds inputs with text type")                    { get('Label text').should == 'id-text' }
      it("finds inputs where label has problem chars")     { get("Label text's got an apostrophe").should == 'id-problem-text' }
    end

  end

  describe '#select' do
    subject{ :select }
    it("finds selects by id")             { get('select-with-id').should == 'select-with-id-data' }
    it("finds selects by name")           { get('select-with-name').should == 'select-with-name-data' }
    it("finds selects by label")          { get('Select with label').should == 'select-with-label-data' }
    it("finds selects by parent label")   { get('Select with parent label').should == 'select-with-parent-label-data' }
    it("casts to string")                 { get(:'Select with parent label').should == 'select-with-parent-label-data' }
  end

  describe '#checkbox' do
    subject{ :checkbox }
    it("finds checkboxes by id")           { get('input-checkbox-with-id').should == 'input-checkbox-with-id-data' }
    it("finds checkboxes by name")         { get('input-checkbox-with-name').should == 'input-checkbox-with-name-data' }
    it("finds checkboxes by label")        { get('Input checkbox with label').should == 'input-checkbox-with-label-data' }
    it("finds checkboxes by parent label") { get('Input checkbox with parent label').should == 'input-checkbox-with-parent-label-data' }
    it("casts to string")                  { get(:'Input checkbox with parent label').should == 'input-checkbox-with-parent-label-data' }
  end

  describe '#radio_button' do
    subject{ :radio_button }
    it("finds radio buttons by id")           { get('input-radio-with-id').should == 'input-radio-with-id-data' }
    it("finds radio buttons by name")         { get('input-radio-with-name').should == 'input-radio-with-name-data' }
    it("finds radio buttons by label")        { get('Input radio with label').should == 'input-radio-with-label-data' }
    it("finds radio buttons by parent label") { get('Input radio with parent label').should == 'input-radio-with-parent-label-data' }
    it("casts to string")                     { get(:'Input radio with parent label').should == 'input-radio-with-parent-label-data' }
  end

  describe '#file_field' do
    subject{ :file_field }
    it("finds file fields by id")           { get('input-file-with-id').should == 'input-file-with-id-data' }
    it("finds file fields by name")         { get('input-file-with-name').should == 'input-file-with-name-data' }
    it("finds file fields by label")        { get('Input file with label').should == 'input-file-with-label-data' }
    it("finds file fields by parent label") { get('Input file with parent label').should == 'input-file-with-parent-label-data' }
    it("casts to string")                   { get(:'Input file with parent label').should == 'input-file-with-parent-label-data' }
  end

  describe "#optgroup" do
    subject { :optgroup }
    it("finds optgroups by label")             { get('Group A').should == 'optgroup-a' }
    it("finds optgroups by approximate label") { get('oup A').should == 'optgroup-a' }
    it("casts to string")                      { get(:'Group A').should == 'optgroup-a' }

    context "with exact match", :type => :exact do
      it("finds by label")                     { get('Group A').should == 'optgroup-a' }
      it("does not find by approximate label") { get('oup A').should be_nil }
    end
  end

  describe '#option' do
    subject{ :option }
    it("finds by text")             { get('Option with text').should == 'option-with-text-data' }
    it("finds by approximate text") { get('Option with').should == 'option-with-text-data' }
    it("casts to string")           { get(:'Option with text').should == 'option-with-text-data' }

    context "with exact match", :type => :exact do
      it("finds by text")                     { get('Option with text').should == 'option-with-text-data' }
      it("does not find by approximate text") { get('Option with').should be_nil }
    end
  end

  describe "#table" do
    subject {:table}
    it("finds by id")                  { get('table-with-id').should == 'table-with-id-data' }
    it("finds by caption")             { get('Table with caption').should == 'table-with-caption-data' }
    it("finds by approximate caption") { get('Table with').should == 'table-with-caption-data' }
    it("casts to string")              { get(:'Table with caption').should == 'table-with-caption-data' }

    context "with exact match", :type => :exact do
      it("finds by caption")                     { get('Table with caption').should == 'table-with-caption-data' }
      it("does not find by approximate caption") { get('Table with').should be_nil }
    end
  end

  describe "#definition_description" do
    subject {:definition_description}
    let(:template) {'stuff'}
    it("find definition description by id")   { get('latte').should == "with-id" }
    it("find definition description by term") { get("Milk").should == "with-dt" }
    it("casts to string")                     { get(:"Milk").should == "with-dt" }
  end
end
