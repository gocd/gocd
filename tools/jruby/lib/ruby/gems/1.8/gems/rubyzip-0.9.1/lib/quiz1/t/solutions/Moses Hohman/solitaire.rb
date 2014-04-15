#!/usr/bin/env ruby
require 'cipher'
require 'yaml'

module Solitaire
	text = ARGV.join(" ")
	if FileTest::readable?("deck.yaml")
		deck = Deck.new(YAML::load(File.open("deck.yaml")))
	else
		deck = Deck.new
	end
	cipher = Cipher.new(text, deck)
	puts "#{cipher.mode}ed: #{cipher.crypt}"
end
