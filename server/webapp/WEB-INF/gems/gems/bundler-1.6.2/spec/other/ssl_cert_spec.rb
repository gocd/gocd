require 'spec_helper'
require 'bundler/ssl_certs/certificate_manager'

describe "SSL Certificates", :if => (ENV['RGV'] == "master") do
  it "are up to date with Rubygems" do
    rubygems = File.expand_path("../../../tmp/rubygems", __FILE__)
    manager = Bundler::SSLCerts::CertificateManager.new(rubygems)
    expect(manager).to be_up_to_date
  end
end
