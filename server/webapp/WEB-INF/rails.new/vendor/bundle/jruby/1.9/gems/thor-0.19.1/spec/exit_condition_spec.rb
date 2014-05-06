require "helper"
require "thor/base"

describe "Exit conditions" do
  it "exits 0, not bubble up EPIPE, if EPIPE is raised" do
    epiped = false

    command = Class.new(Thor) do
      desc "my_action", "testing EPIPE"
      define_method :my_action do
        epiped = true
        fail Errno::EPIPE
      end
    end

    expect { command.start(["my_action"]) }.to raise_error(SystemExit)
    expect(epiped).to eq(true)
  end
end
