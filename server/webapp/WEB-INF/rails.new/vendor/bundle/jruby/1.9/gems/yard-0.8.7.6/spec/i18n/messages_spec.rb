require File.dirname(__FILE__) + '/../spec_helper'

describe YARD::I18n::Messages do
  def message(id)
    YARD::I18n::Message.new(id)
  end

  def messages
    YARD::I18n::Messages.new
  end

  before do
    @messages = messages
  end

  describe "#each" do
    it "should enumerate Message" do
      @messages.register("Hello World!")
      @messages.register("Title")
      enumerated_messages = []
      @messages.each do |message|
        enumerated_messages << message
      end
      enumerated_messages = enumerated_messages.sort_by {|m| m.id }
      enumerated_messages.should == [message("Hello World!"), message("Title")]
    end

    it "should not any Message for empty messages" do
      enumerated_messages = []
      @messages.each do |message|
        enumerated_messages << message
      end
      enumerated_messages.should == []
    end
  end

  describe "#[]" do
    it "should return registered message" do
      @messages.register("Hello World!")
      @messages["Hello World!"].should == message("Hello World!")
    end

    it "should return for nonexistent message ID" do
      @messages["Hello World!"].should == nil
    end
  end

  describe "#register" do
    it "should return registered message" do
      @messages.register("Hello World!").should == message("Hello World!")
    end

    it "should return existent message" do
      message = @messages.register("Hello World!")
      @messages.register("Hello World!").object_id.should == message.object_id
    end
  end

  describe "#==" do
    it "should return true for same value messages" do
      @messages.register("Hello World!")
      other_messages = messages
      other_messages.register("Hello World!")
      @messages.should == other_messages
    end
  end
end
