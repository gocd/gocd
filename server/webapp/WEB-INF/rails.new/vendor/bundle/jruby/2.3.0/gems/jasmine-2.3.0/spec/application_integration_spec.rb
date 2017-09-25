require 'spec_helper'

describe "Jasmine::Application" do

  it "includes no-cache headers for specs" do
    pending
    get "/__spec__/example_spec.js"
    last_response.headers.should have_key("Cache-Control")
    last_response.headers["Cache-Control"].should == "max-age=0, private, must-revalidate"
    last_response.headers['Pragma'].each do |key|
      last_response.headers[key].should == 'no-cache'
    end
  end

end
