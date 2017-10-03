require 'spec_helper'

describe Jasmine::Formatters::Multi do
  it "should delegate to the passed in formatters" do
    formatter1 = double(:formatter1)
    formatter2 = double(:formatter2)
    multi = Jasmine::Formatters::Multi.new([formatter1, formatter2])

    results1 = double(:results1)

    expect(formatter1).to receive(:format).with(results1)
    expect(formatter2).to receive(:format).with(results1)
    multi.format(results1)

    results2 = double(:results1)

    expect(formatter1).to receive(:format).with(results2)
    expect(formatter2).to receive(:format).with(results2)
    multi.format(results2)

    run_details = double(:run_details)

    expect(formatter1).to receive(:done).with(run_details)
    expect(formatter2).to receive(:done).with(run_details)
    multi.done(run_details)
  end
end
