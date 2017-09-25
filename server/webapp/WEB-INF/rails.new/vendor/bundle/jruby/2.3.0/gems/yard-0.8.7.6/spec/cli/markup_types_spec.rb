require File.dirname(__FILE__) + '/../spec_helper'

describe YARD::CLI::MarkupTypes do
  it "lists all available markup types" do
    YARD::CLI::MarkupTypes.run
    data = log.io.string
    exts = YARD::Templates::Helpers::MarkupHelper::MARKUP_EXTENSIONS
    YARD::Templates::Helpers::MarkupHelper::MARKUP_PROVIDERS.each do |name, providers|
      data.should match(/\b#{name}\b/)

      # Match all extensions
      exts[name].each do |ext|
        data.should include(".#{ext}")
      end if exts[name]

      # Match all provider libs
      providers.each do |provider|
        data.should match(/\b#{provider[:lib]}\b/)
      end
    end
  end
end
