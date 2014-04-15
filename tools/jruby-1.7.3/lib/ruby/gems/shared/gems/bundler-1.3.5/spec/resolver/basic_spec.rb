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

  it "resolve a conflicting index" do
    @index = a_conflict_index
    dep "my_app"
    should_resolve_as %w(activemodel-3.2.11 builder-3.0.4 grape-0.2.6 my_app-1.0.0)
  end
end
