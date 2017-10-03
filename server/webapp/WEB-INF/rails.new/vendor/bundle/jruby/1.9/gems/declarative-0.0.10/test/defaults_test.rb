require "test_helper"

class DefaultsOptionsTest < Minitest::Spec
  let (:song) { Struct.new(:title, :author_name, :song_volume, :description).new("Revolution", "Some author", 20, nil) }
  let (:schema) { Declarative::Definitions.new(Declarative::Definitions::Definition).extend Declarative::Definitions::Inspect }
  let (:defaults) { Declarative::Defaults.new }

  describe "hash options combined with dynamic options" do
    it do
      defaults.merge!(render_nil: true) do |name|
        { as: name.to_s.upcase }
      end

      schema.add :title, _defaults: defaults
      schema.add :author_name
      schema.add :description, _defaults: defaults

      schema.inspect.must_equal '{"title"=>#<Declarative::Definitions::Definition: @options={:render_nil=>true, :as=>"TITLE", :name=>"title"}>, "author_name"=>#<Declarative::Definitions::Definition: @options={:name=>"author_name"}>, "description"=>#<Declarative::Definitions::Definition: @options={:render_nil=>true, :as=>"DESCRIPTION", :name=>"description"}>}'
    end
  end

  describe "with only dynamic property options" do
    it do
      defaults.merge!({}) do |name|
        { as: name.to_s.upcase }
      end

      schema.add :title, _defaults: defaults
      schema.add :author_name
      schema.add :description, _defaults: defaults

      schema.inspect.must_equal '{"title"=>#<Declarative::Definitions::Definition: @options={:as=>"TITLE", :name=>"title"}>, "author_name"=>#<Declarative::Definitions::Definition: @options={:name=>"author_name"}>, "description"=>#<Declarative::Definitions::Definition: @options={:as=>"DESCRIPTION", :name=>"description"}>}'
    end
  end

  describe "with only hashes" do
    it do
      defaults.merge!(render_nil: true)

      schema.add :title, _defaults: defaults
      schema.add :author_name
      schema.add :description, _defaults: defaults

      schema.inspect.must_equal '{"title"=>#<Declarative::Definitions::Definition: @options={:render_nil=>true, :name=>"title"}>, "author_name"=>#<Declarative::Definitions::Definition: @options={:name=>"author_name"}>, "description"=>#<Declarative::Definitions::Definition: @options={:render_nil=>true, :name=>"description"}>}'
    end
  end

  describe "#add options win" do
    it do
      defaults.merge!(render_nil: true) do |name|
        { as: name.to_s.upcase }
      end

      schema.add :title, as: "Title", _defaults: defaults
      schema.add :author_name
      schema.add :description, _defaults: defaults

      schema.inspect.must_equal '{"title"=>#<Declarative::Definitions::Definition: @options={:render_nil=>true, :as=>"Title", :name=>"title"}>, "author_name"=>#<Declarative::Definitions::Definition: @options={:name=>"author_name"}>, "description"=>#<Declarative::Definitions::Definition: @options={:render_nil=>true, :as=>"DESCRIPTION", :name=>"description"}>}'
    end
  end


  describe "multiple Defaults#merge!" do
    it "merges arrays automatically" do
      defaults.merge!(a: 1, b: 2)
      defaults.merge!(      b: 3, _features: Declarative::Variables::Append(["A"]) )
      defaults.merge!(            _features: Declarative::Variables::Append(["B", "C"]) )
      defaults.(nil, {}).inspect.must_equal "{:a=>1, :b=>3, :_features=>[\"A\", \"B\", \"C\"]}"
    end

    it "what" do
      defaults.merge!( _features: Declarative::Variables::Append(["A"]) ) do |name, options|
        { _features: Declarative::Variables::Append( ["B", "D"] ) }
      end

      defaults.(nil, {}).inspect.must_equal "{:_features=>[\"A\", \"B\", \"D\"]}"
    end
  end

  describe "deprecation" do
    require 'stringio'
    before do
      @old_stderr = $stderr
      $stderr = StringIO.new
    end

    after { $stderr = @old_stderr }

    it "prints deprecation twice" do
      defaults.merge!( _features: ["A"] ) do |name, options|
        { _features: ["B", "D"] }
      end

      defaults.(nil, {}).inspect.must_equal "{:_features=>[\"A\", \"B\", \"D\"]}"

      $stderr.string.must_equal %{[Declarative] Defaults#merge! and #call still accept arrays and automatically prepend those. This is now deprecated, you should replace `ary` with `Declarative::Variables::Append(ary)`.\n}*2
    end
  end
end
