require 'test_helper'

class SerializeDeserializeTest < BaseTest
  subject { Struct.new(:song).new.extend(representer) }

  describe "deserialize" do
    representer! do
      property :song,
        :instance => lambda { |fragment, *| fragment.to_s.upcase },
        :prepare  => lambda { |fragment, *| fragment }, # TODO: allow false.
        :deserialize => lambda { |object, fragment, args|
          "#{object} #{fragment} #{args.inspect}"
        }
    end

    it { subject.from_hash({"song" => Object}, {:volume => 9}).song.must_equal "OBJECT Object {:volume=>9}" }
  end

  describe "serialize" do
    representer! do
      property :song,
        :representable => true,
        :prepare  => lambda { |fragment, *| fragment }, # TODO: allow false.
        :serialize => lambda { |object, args|
          "#{object} #{args.inspect}"
        }
    end

    before { subject.song = "Arrested In Shanghai" }

    it { subject.to_hash({:volume => 9}).must_equal({"song"=>"Arrested In Shanghai {:volume=>9}"}) }
  end
end