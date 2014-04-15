require 'spec_helper'

main = self
describe "The describe method" do
  it 'is available on the main object' do
    main.should respond_to(:describe)
  end

  it 'is available on modules (so example groups can be nested inside them)' do
    Module.new.should respond_to(:describe)
  end

  it 'is not available on other types of objects' do
    Object.new.should_not respond_to(:describe)
  end
end

