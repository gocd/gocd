SPORK_SPEC_DIR = File.expand_path( "../../../spec/", File.dirname(__FILE__))
Dir.glob("#{SPORK_SPEC_DIR}/support/*.rb").each { |f| require(f) }
