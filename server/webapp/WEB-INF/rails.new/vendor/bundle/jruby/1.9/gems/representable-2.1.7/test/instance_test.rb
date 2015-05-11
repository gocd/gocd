require 'test_helper'

class InstanceTest < BaseTest
  Song = Struct.new(:id, :title)
  Song.class_eval do
    def self.find(id)
      new(id, "Invincible")
    end
  end

  describe "lambda { fragment } (new way of class: lambda { nil })" do
    representer! do
      property :title, :instance  => lambda { |fragment, args| fragment }
    end

    it "skips creating new instance" do
      object = Object.new
      object.instance_eval do
        def from_hash(hash, *args)
          hash
        end
      end

      song = OpenStruct.new.extend(representer).from_hash(hash = {"title" => object})
      song.title.must_equal object
    end
  end


# TODO: use *args in from_hash.
# DISCUSS: do we need parse_strategy?
  describe "property with :instance" do
    representer!(:inject => :song_representer) do
      property :song,
        :instance => lambda { |fragment, *args| fragment["id"] == song.id ? song : Song.find(fragment["id"]) },
        :extend => song_representer
    end

    it { OpenStruct.new(:song => Song.new(1, "The Answer Is Still No")).extend(representer).
      from_hash("song" => {"id" => 1}).song.must_equal Song.new(1, "The Answer Is Still No") }

    it { OpenStruct.new(:song => Song.new(1, "The Answer Is Still No")).extend(representer).
      from_hash("song" => {"id" => 2}).song.must_equal Song.new(2, "Invincible") }
  end


  describe "collection with :instance" do
    representer!(:inject => :song_representer) do
      collection :songs,
        :instance => lambda { |fragment, i, *args|

          fragment["id"] == songs[i].id ? songs[i] : Song.find(fragment["id"])
        }, # let's not allow returning nil anymore. make sure we can still do everything as with nil. also, let's remove parse_strategy: sync.

        :extend => song_representer
    end

    it {
      album= Struct.new(:songs).new(songs = [
      Song.new(1, "The Answer Is Still No"),
      Song.new(2, "")])

      album.
        extend(representer).
        from_hash("songs" => [{"id" => 2},{"id" => 2, "title"=>"The Answer Is Still No"}]).songs.must_equal [
          Song.new(2, "Invincible"), Song.new(2, "The Answer Is Still No")]
    }
  end

  describe "property with lambda receiving fragment and args" do
    representer!(:inject => :song_representer) do
      property :song, :instance => lambda { |fragment, args| Struct.new(:args, :id).new([fragment, args]) }, :extend => song_representer
    end

    it { OpenStruct.new(:song => Song.new(1, "The Answer Is Still No")).extend(representer).
      from_hash({"song" => {"id" => 1}}, {:volume => 1}).song.args.must_equal([{"id"=>1}, {:volume=>1}]) }
  end

  # TODO: raise and test instance:{nil}
  # describe "property with instance: { nil }" do # TODO: introduce :representable option?
  #   representer!(:inject => :song_representer) do
  #     property :song, :instance => lambda { |*| nil }, :extend => song_representer
  #   end

  #   let (:hit) { hit = OpenStruct.new(:song => song).extend(representer) }

  #   it "calls #to_hash on song instance, nothing else" do
  #     hit.to_hash.must_equal("song"=>{"title"=>"Resist Stance"})
  #   end

  #   it "calls #from_hash on the existing song instance, nothing else" do
  #     song_id = hit.song.object_id
  #     hit.from_hash("song"=>{"title"=>"Suffer"})
  #     hit.song.title.must_equal "Suffer"
  #     hit.song.object_id.must_equal song_id
  #   end
  # end

  # lambda { |fragment, i, Context(binding: <..>, args: [..])| }

  describe "sync" do
    representer!(:inject => :song_representer) do
      collection :songs,
        :instance => lambda { |fragment, i, *args|
          songs[i]
        },
        :extend => song_representer,
        # :parse_strategy => :sync
        :setter => lambda { |*|  }
    end

    it {
      album= Struct.new(:songs).new(songs = [
      Song.new(1, "The Answer Is Still No"),
      Song.new(2, "Invncble")])

      album.
        extend(representer).
        from_hash("songs" => [{"title" => "The Answer Is Still No"}, {"title" => "Invincible"}])

      album.songs.must_equal [
        Song.new(1, "The Answer Is Still No"),
        Song.new(2, "Invincible")]

      songs.object_id.must_equal album.songs.object_id
      songs[0].object_id.must_equal album.songs[0].object_id
      songs[1].object_id.must_equal album.songs[1].object_id
    }
  end

  describe "update existing elements, only" do
    representer!(:inject => :song_representer) do
      collection :songs,
        :instance => lambda { |fragment, i, *args|

          #fragment["id"] == songs[i].id ? songs[i] : Song.find(fragment["id"])
          songs.find { |s| s.id == fragment["id"] }
        }, # let's not allow returning nil anymore. make sure we can still do everything as with nil. also, let's remove parse_strategy: sync.

        :extend => song_representer,
        # :parse_strategy => :sync
        :setter => lambda { |*|  }
    end

    it {
      album= Struct.new(:songs).new(songs = [
      Song.new(1, "The Answer Is Still No"),
      Song.new(2, "Invncble")])

      album.
        extend(representer).
        from_hash("songs" => [{"id" => 2, "title" => "Invincible"}]).
        songs.must_equal [
          Song.new(1, "The Answer Is Still No"),
          Song.new(2, "Invincible")]
          # TODO: check elements object_id!

      songs.object_id.must_equal album.songs.object_id
      songs[0].object_id.must_equal album.songs[0].object_id
      songs[1].object_id.must_equal album.songs[1].object_id
    }
  end


  describe "add incoming elements, only" do
    representer!(:inject => :song_representer) do
      collection :songs,
        :instance => lambda { |fragment, i, *args|
          songs << song=Song.new(2)
          song
        }, # let's not allow returning nil anymore. make sure we can still do everything as with nil. also, let's remove parse_strategy: sync.

        :extend => song_representer,
        # :parse_strategy => :sync
        :setter => lambda { |*|  }
    end

    it {
      album= Struct.new(:songs).new(songs = [
        Song.new(1, "The Answer Is Still No")])

      album.
        extend(representer).
        from_hash("songs" => [{"title" => "Invincible"}]).
        songs.must_equal [
          Song.new(1, "The Answer Is Still No"),
          Song.new(2, "Invincible")]

      songs.object_id.must_equal album.songs.object_id
      songs[0].object_id.must_equal album.songs[0].object_id
    }
  end


  # not sure if this must be a library strategy
  describe "replace existing element" do
    representer!(:inject => :song_representer) do
      collection :songs,
        :instance => lambda { |fragment, i, *args|
          id = fragment.delete("replace_id")
          replaced = songs.find { |s| s.id == id }
          songs[songs.index(replaced)] = song=Song.new(3)
          song
        }, # let's not allow returning nil anymore. make sure we can still do everything as with nil. also, let's remove parse_strategy: sync.

        :extend => song_representer,
        # :parse_strategy => :sync
        :setter => lambda { |*|  }
    end

    it {
      album= Struct.new(:songs).new(songs = [
        Song.new(1, "The Answer Is Still No"),
        Song.new(2, "Invincible")])

      album.
        extend(representer).
        from_hash("songs" => [{"replace_id"=>2, "id" => 3, "title" => "Soulmate"}]).
        songs.must_equal [
          Song.new(1, "The Answer Is Still No"),
          Song.new(3, "Soulmate")]

      songs.object_id.must_equal album.songs.object_id
      songs[0].object_id.must_equal album.songs[0].object_id
    }
  end


  describe "replace collection" do
    representer!(:inject => :song_representer) do
      collection :songs,
        :extend => song_representer, :class => Song
    end

    it {
      album= Struct.new(:songs).new(songs = [
        Song.new(1, "The Answer Is Still No")])

      album.
        extend(representer).
        from_hash("songs" => [{"title" => "Invincible"}]).
        songs.must_equal [
          Song.new(nil, "Invincible")]

      songs.object_id.wont_equal album.songs.object_id
    }
  end


  describe "new syntax for instance: true" do
    representer!(:inject => :song_representer) do
      property :song, :pass_options => true,
        :extend => song_representer, :instance => lambda { |fragment, args| args.binding.get }
    end

    it "uses Binding#get instead of creating an instance, but deprecates" do
      album= Struct.new(:song).new(song = Song.new(1, "The Answer Is Still No"))

      album.
        extend(representer).
        from_hash("song" => {"title" => "Invincible"}).
        song.must_equal Song.new(1, "Invincible")

      album.song.object_id.must_equal song.object_id
    end
  end
end