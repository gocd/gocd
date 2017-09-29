shared_examples_for "an RSpec matcher" do |options|
  let(:valid_value)   { options.fetch(:valid_value) }
  let(:invalid_value) { options.fetch(:invalid_value) }
  let(:deprecations)  { [] }

  def matched_deprecations
    deprecations.select { |opts| opts[:deprecated] =~ /matcher == value/ }
  end

  before do
    allow(RSpec.configuration.reporter).to receive(:deprecation) do |opts|
      deprecations << opts
    end
  end

  it 'matches a valid value when using #== so it can be composed' do
    expect(matcher).to eq(valid_value)
  end

  it 'matches a valid value when using #=== so it can be composed' do
    expect(matcher).to be === valid_value
  end

  it 'does not match an invalid value when using #== so it can be composed' do
    expect(matcher).not_to eq(invalid_value)
  end

  it 'does not match an invalid value when using #=== so it can be composed' do
    expect(matcher).not_to be === invalid_value
  end

  it 'does not print a deprecation warning when using #===' do
    matcher === valid_value
    matcher === invalid_value
    expect(matched_deprecations).to eq([])
  end

  it 'does not print a deprecation warning when using #== if it returns false' do
    (matcher == invalid_value).nil? # calling a method to avoid a warning
    expect(matched_deprecations).to eq([])
  end

  it 'does not print a deprecation warning when using #== if it returns true because it was given the same object' do
    expect(matcher).to be == matcher
    expect(matched_deprecations).to eq([])
  end

  it 'prints a deprecation warning for #== when given a valid value since' do
    (matcher == valid_value).nil? # calling a method to avoid a warning
    expect(matched_deprecations.count).to eq(1)
    deprecation = matched_deprecations.first
    expect(deprecation[:call_site]).to include([__FILE__, __LINE__ - 3].join(':'))
  end
end

