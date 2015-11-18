require 'test_helper'
require "uber/builder"

class BuilderTest < MiniTest::Spec
  Evergreen = Struct.new(:title)
  Hit  = Struct.new(:title)

  class Song
    include Uber::Builder

    builds do |options|
      if options[:evergreen]
        Evergreen
      elsif options[:hit]
        Hit
      end
    end

    def self.build(options)
      class_builder.call(options).new
    end
  end

  # building class if no block matches
  it { Song.build({}).must_be_instance_of Song }

  it { Song.build({evergreen: true}).must_be_instance_of Evergreen }
  it { Song.build({hit: true}).must_be_instance_of Hit }

  # test chained builds.
  class Track
    include Uber::Builder

    builds do |options|
      Evergreen if options[:evergreen]
    end

    builds do |options|
      Hit if options[:hit]
    end

    def self.build(options)
      class_builder.call(options).new
    end
  end

  it { Track.build({}).must_be_instance_of Track }
  it { Track.build({evergreen: true}).must_be_instance_of Evergreen }
  it { Track.build({hit: true}).must_be_instance_of Hit }


  # test inheritance. builder do not inherit.
  class Play < Song
  end

  it { Play.build({}).must_be_instance_of Play }
  it { Play.build({evergreen: true}).must_be_instance_of Play }
  it { Play.build({hit: true}).must_be_instance_of Play }

  # test return from builds
  class Boomerang
    include Uber::Builder

    builds ->(options) do
      return Song if options[:hit]
    end

    def self.build(options)
      class_builder.call(options).new
    end
  end

  it { Boomerang.build({}).must_be_instance_of Boomerang }
  it { Boomerang.build({hit: true}).must_be_instance_of Song }
end