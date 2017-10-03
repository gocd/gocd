require 'spec_helper'

describe Jasmine::Formatters::ExitCode do
  subject(:formatter) { Jasmine::Formatters::ExitCode.new }

  it 'is successful with no results' do
    formatter.done({})
    expect(formatter).to be_succeeded
  end

  it 'is unsuccessful with a failure' do
    formatter.format([double(:result, failed?: true)])
    formatter.done({})
    expect(formatter).not_to be_succeeded
  end

  it 'finds a single failure to cause the error' do
    formatter.format([double(failed?: false), double(failed?: false), double(failed?: true), double(failed?: false)])
    formatter.done({})
    expect(formatter).not_to be_succeeded
  end

  it 'is unsuccessful with a failedExpectation in done' do
    formatter.done({
      'failedExpectations' => [{}]
    })
    expect(formatter).not_to be_succeeded
  end

  it 'is still successful with empty failedExpectations in done' do
    formatter.done({ 'failedExpectations' => [] })
    expect(formatter).to be_succeeded
  end
end
