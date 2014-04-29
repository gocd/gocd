require "spec_helper"

describe "Resolving" do

  before :each do
    @index = an_awesome_index
  end

  it "resolves a single gem" do
    dep "rack"

    should_resolve_as %w(rack-1.1)
  end

  it "resolves a gem with dependencies" do
    dep "actionpack"

    should_resolve_as %w(actionpack-2.3.5 activesupport-2.3.5 rack-1.0)
  end

  it "resolves a conflicting index" do
    @index = a_conflict_index
    dep "my_app"
    should_resolve_as %w(activemodel-3.2.11 builder-3.0.4 grape-0.2.6 my_app-1.0.0)
  end

  it "resolves a complex conflicting index" do
    @index = a_complex_conflict_index
    dep "my_app"
    should_resolve_as %w(a-1.4.0 b-0.3.5 c-3.2 d-0.9.8 my_app-1.1.0)
  end

  it "resolves a index with conflict on child" do
    @index = index_with_conflict_on_child
    dep "chef_app"
    should_resolve_as %w(berkshelf-2.0.7 chef-10.26 chef_app-1.0.0 json-1.7.7)
  end

  it "resolves a index with root level conflict on child" do
    @index = a_index_with_root_conflict_on_child
    dep "i18n", "~> 0.4"
    dep "activesupport", "~> 3.0"
    dep "activerecord", "~> 3.0"
    dep "builder", "~> 2.1.2"
    should_resolve_as %w(activesupport-3.0.5 i18n-0.4.2 builder-2.1.2 activerecord-3.0.5 activemodel-3.0.5)
  end

  it "raises an exception if a child dependency is not resolved" do
    @index = a_unresovable_child_index
    dep "chef_app_error"
    expect {
      resolve
    }.to raise_error(Bundler::VersionConflict)
  end

  it "should throw error in case of circular dependencies" do
    @index = a_circular_index
    dep "circular_app"

    got = resolve
    expect {
      got = got.map { |s| s.full_name }.sort
    }.to raise_error(Bundler::CyclicDependencyError, /please remove either gem 'foo' or gem 'bar'/i)
  end

end
