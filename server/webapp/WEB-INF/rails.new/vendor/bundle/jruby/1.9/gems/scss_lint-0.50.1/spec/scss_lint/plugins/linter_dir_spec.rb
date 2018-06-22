require 'spec_helper'

describe SCSSLint::Plugins::LinterDir do
  let(:plugin_directory) { File.expand_path('../../fixtures/plugins', __FILE__) }
  let(:subject) { described_class.new(plugin_directory) }

  describe '#config' do
    it 'returns empty configuration' do
      subject.config.should == SCSSLint::Config.new({})
    end
  end

  describe '#load' do
    it 'requires each file in the plugin directory' do
      subject.should_receive(:require)
             .with(File.join(plugin_directory, 'linter_plugin.rb')).once

      subject.load
    end
  end
end
