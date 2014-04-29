require 'spec_helper'

describe "An RSpec Mock" do
  it "hides internals in its inspect representation" do
    m = double('cup')
    expect(m.inspect).to match(/#<RSpec::Mocks::Mock:0x[a-f0-9.]+ @name="cup">/)
  end
end
