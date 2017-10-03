require "test_helper"

class DefinitionsTest < Minitest::Spec
  NestedBuilder = ->(options) {
    base = options[:_base] || Declarative::Definitions.new(Declarative::Definitions::Definition)
    base.instance_exec(&options[:_block])
    base
  }

  let (:schema) { Declarative::Definitions.new(Declarative::Definitions::Definition).extend(Declarative::Definitions::Inspect) }

  it "what" do
    # #add works with name
    schema.add :id
    # get works with symbol
    schema.get(:id).inspect.must_equal '#<Declarative::Definitions::Definition: @options={:name=>"id"}>'
    # get works with string
    schema.get("id").inspect.must_equal '#<Declarative::Definitions::Definition: @options={:name=>"id"}>'

    # #add with name and options
    schema.add(:id, unique: true)
    schema.get(:id).inspect.must_equal '#<Declarative::Definitions::Definition: @options={:unique=>true, :name=>"id"}>'
  end

  it "overwrites old when called twice" do
    schema.add :id
    schema.add :id, cool: true
    schema.inspect.must_equal '{"id"=>#<Declarative::Definitions::Definition: @options={:cool=>true, :name=>"id"}>}'
  end

  it "#add with block" do
    schema.add :artist, _nested_builder: NestedBuilder do
      add :name
      add :band, _nested_builder: NestedBuilder do
        add :location
      end
    end

    schema.inspect.must_equal '{"artist"=>#<Declarative::Definitions::Definition: @options={:nested=>{"name"=>#<Declarative::Definitions::Definition: @options={:name=>"name"}>, "band"=>#<Declarative::Definitions::Definition: @options={:nested=>{"location"=>#<Declarative::Definitions::Definition: @options={:name=>"location"}>}, :name=>"band"}>}, :name=>"artist"}>}'
  end

  it "#add with :nested instead of block" do
    nested_schema = Declarative::Definitions.new(Declarative::Definitions::Definition)
    nested_schema.extend(Declarative::Definitions::Inspect)

    nested_schema.add :name

    schema.add :artist, nested: nested_schema

    schema.inspect.must_equal '{"artist"=>#<Declarative::Definitions::Definition: @options={:nested=>{"name"=>#<Declarative::Definitions::Definition: @options={:name=>"name"}>}, :name=>"artist"}>}'
  end


  it "#add with inherit: true and block" do
    schema.add :artist, cool: true, _nested_builder: NestedBuilder do
      add :name
      add :band, crazy: nil, _nested_builder: NestedBuilder do
        add :location
      end
    end

    schema.add :id, unique: true, value: 1

    schema.add :artist, uncool: false, _nested_builder: NestedBuilder, inherit: true do
      add :band, normal: false, _nested_builder: NestedBuilder, inherit: true do
        add :genre
      end
    end

    schema.add :id, unique: false, inherit: true

    schema.inspect.must_equal '{"artist"=>#<Declarative::Definitions::Definition: @options={:cool=>true, :nested=>{"name"=>#<Declarative::Definitions::Definition: @options={:name=>"name"}>, "band"=>#<Declarative::Definitions::Definition: @options={:crazy=>nil, :nested=>{"location"=>#<Declarative::Definitions::Definition: @options={:name=>"location"}>, "genre"=>#<Declarative::Definitions::Definition: @options={:name=>"genre"}>}, :name=>"band", :normal=>false}>}, :name=>"artist", :uncool=>false}>, "id"=>#<Declarative::Definitions::Definition: @options={:unique=>false, :value=>1, :name=>"id"}>}'
  end

  it "#add with nested options followed by inherit: true" do
    schema.add :id, deserializer: options = { render: false }
    schema.add :id, inherit: true

    schema.get(:id)[:deserializer][:parse] = true

    options.must_equal(render: false)
  end
end


class DefinitionTest < Minitest::Spec
  let (:definition) { Declarative::Definitions::Definition.new(:name) }

  it "#merge does return deep copy" do
    options = { render: false }
    merged = definition.merge(options)
    definition.merge!(render: true)
    merged.must_equal(:name=>"name", render: false)
  end
end