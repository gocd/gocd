require 'test_helper'

class FeaturesTest < MiniTest::Spec
  module Title
    def title; "Is It A Lie"; end
  end
  module Length
    def length; "2:31"; end
  end

  definition = lambda {
    feature Title
    feature Length

    # exec_context: :decorator, so the readers are called on the Decorator instance (which gets readers from features!).
    property :title, exec_context: :decorator
    property :length, exec_context: :decorator
    property :details do
      property :title, exec_context: :decorator
    end
  }

  let (:song) { OpenStruct.new(:details => Object.new) }

  describe "Module" do
    representer! do
      instance_exec &definition
    end

    it { song.extend(representer).to_hash.must_equal({"title"=>"Is It A Lie", "length"=>"2:31", "details"=>{"title"=>"Is It A Lie"}}) }
  end


  describe "Decorator" do
    representer!(:decorator => true) do
      instance_exec &definition
    end

    it { representer.new(song).to_hash.must_equal({"title"=>"Is It A Lie", "length"=>"2:31", "details"=>{"title"=>"Is It A Lie"}}) }
  end
end