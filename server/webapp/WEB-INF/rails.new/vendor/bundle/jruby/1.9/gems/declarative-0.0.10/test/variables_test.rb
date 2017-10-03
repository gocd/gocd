require "test_helper"

class DSLOptionsTest < Minitest::Spec
  let(:defaults) { { id: 1, connections: { first: 1, second: 2 }, list: [3] } }

  Variables = Declarative::Variables

  after do
    Declarative::Inspect(defaults).must_equal %{{:id=>1, :connections=>{:first=>1, :second=>2}, :list=>[3]}}
  end

  #- Merge
  it "merges Merge over original" do
    options = Variables.merge(
      defaults,
      connections: Variables::Merge( second: 3, third: 4 )
    )

    options.must_equal( { id: 1, connections: { first: 1, second: 3, third: 4 }, :list=>[3] } )
  end

  it "accepts Procs" do
    options = Variables.merge(
      defaults,
      connections: proc = ->(*) { raise }
    )

    options.must_equal( { id: 1, connections: proc, :list=>[3] } )
  end

  it "overrides original without Merge" do
    options = Variables.merge(
    defaults, connections: { second: 3, third: 4 } )

    options.must_equal( { id: 1, connections: { second: 3, third: 4 }, :list=>[3] } )
  end

  it "creates new hash if original not existent" do
    options = Variables.merge(
      defaults,
      bla: Variables::Merge( second: 3, third: 4 )
    )

    options.must_equal( {:id=>1, :connections=>{:first=>1, :second=>2}, :list=>[3], :bla=>{:second=>3, :third=>4}} )
  end

  #- Append
  it "appends to Array" do
    options = Variables.merge(
      defaults,
      list: Variables::Append( [3, 4, 5] )
    )

    options.must_equal( { id: 1, connections: { first: 1, second: 2 }, :list=>[3, 3, 4, 5] } )
  end

  it "creates new array if original not existent" do
    options = Variables.merge(
      defaults,
      another_list: Variables::Append( [3, 4, 5] )
    )

    options.must_equal( { id: 1, connections: { first: 1, second: 2 }, :list=>[3], :another_list=>[3, 4, 5] } )
  end
end
