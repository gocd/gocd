require 'spec_helper'

describe Jasmine::Runners::PhantomJs do
  it 'converts a given cli options hash to a cli arguments string' do
    options = {
      'local-storage-quota' => 5000,
      'local-storage-path' => 'tmp/xyz'
    }

    phantom_js = Jasmine::Runners::PhantomJs.new(nil, nil,nil,nil, nil,nil, options)

    expect(phantom_js.cli_options_string).to eq('--local-storage-quota=5000 --local-storage-path=tmp/xyz')
  end
end
