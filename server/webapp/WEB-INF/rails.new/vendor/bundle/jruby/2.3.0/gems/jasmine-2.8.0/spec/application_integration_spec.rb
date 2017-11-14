require 'spec_helper'

describe "Jasmine::Application" do

  it "includes no-cache headers for specs" do
    pending
    get "/__spec__/example_spec.js"
    expect(last_response.headers).to have_key("Cache-Control")
    expect(last_response.headers["Cache-Control"]).to eq "max-age=0, private, must-revalidate"
    last_response.headers['Pragma'].each do |key|
      expect(last_response.headers[key]).to eq 'no-cache'
    end
  end

end
