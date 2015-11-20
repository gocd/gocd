shared_examples_for "an RSpec matcher" do |options|
  let(:valid_value)   { options.fetch(:valid_value) }
  let(:invalid_value) { options.fetch(:invalid_value) }

  it 'matches a valid value when using #== so it can be composed' do
    expect(matcher).to eq(valid_value)
  end

  it 'does not match an invalid value when using #== so it can be composed' do
    expect(matcher).not_to eq(invalid_value)
  end
end

