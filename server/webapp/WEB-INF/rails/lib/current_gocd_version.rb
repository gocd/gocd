# This is a hack because calling static methods on `com.thoughtworks.go.CurrentGoCDVersion` seems to have a perf overhead on jruby
#
class CurrentGoCDVersion
  @@__api_docs_url = Hash.new do |hash, key|
    hash[key] = com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl(key)
  end

  @@__docs_url = Hash.new do |hash, key|
    hash[key] = com.thoughtworks.go.CurrentGoCDVersion.docsUrl(key)
  end

  def self.api_docs_url(fragment)
    @@__api_docs_url[fragment]
  end

  def self.docs_url(suffix)
    @@__docs_url[suffix]
  end
end
