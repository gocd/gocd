require File.dirname(__FILE__) + '/../../../spec_helper'

describe "include_text" do

  it "should have a helpful description" do
    matcher = include_text("foo bar")
    matcher.description.should == 'include text "foo bar"'
  end

  it 'should match if the text is contained' do
    matcher = include_text('big piece')
    matcher.matches?('I am a big piece of text').should be_true
  end

  it 'should not match if text is not contained' do
    matcher = include_text('foo bar')
    matcher.matches?('hello world').should be_false
  end

end

describe "include_text", :type => :controller do
  ['isolation','integration'].each do |mode|
    if mode == 'integration'
      integrate_views
    end

    describe "where target is a response (in #{mode} mode)" do
      controller_name :render_spec

      it "should pass with exactly matching text" do
        post 'text_action'
        response.should include_text("this is the text for this action")
      end

      it 'should pass with substring matching text' do
        post 'text_action'
        response.should include_text('text for this')
      end

      it "should fail with incorrect text" do
        post 'text_action'
        lambda {
          response.should include_text("the accordian guy")
        }.should fail_with("expected to find \"the accordian guy\" in \"this is the text for this action\"")
      end

      it "should pass using should_not with incorrect text" do
        post 'text_action'
        response.should_not include_text("the accordian guy")
      end

      it "should fail when a template is rendered" do
        get 'some_action'
        lambda {
          response.should include_text("this is the text for this action")
        }.should fail_with(/expected to find \"this is the text for this action\"/)
      end
    end
  end
end

