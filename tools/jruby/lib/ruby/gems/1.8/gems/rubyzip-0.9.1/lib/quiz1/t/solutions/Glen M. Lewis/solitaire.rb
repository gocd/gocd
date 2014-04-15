#!/usr/bin/env ruby
# Solitaire Cipher
# Ruby-lang quiz #1
# Solution by Glenn M. Lewis - 9/27/04

$debug = nil

class Card
  attr_reader :value, :face_value, :suit

  def initialize(face_value, suit)
    @face_value = face_value
    @suit = suit
    if suit then
      @value = calc_value(face_value, suit)
      return
    end
    case face_value
    when "AJoker"
      @value = 53
    when "BJoker"
      @value = 53
    else
      puts "ERROR: Unknown joker: #{joker}, should be 'AJoker' or 'BJoker'"
      exit
    end
  end

  def calc_value(face_value, suit)
    val = 0
    case suit
    when "S" then val += 39
    when "H" then val += 26
    when "D" then val += 13
    when "C"
    else
      puts "ERROR: Unknown suit: #{suit}, should be C,D,H,S"
    end
    case face_value
    when 2..10 then val += face_value
    when "A" then val += 1
    when "J" then val += 11
    when "Q" then val += 12
    when "K" then val += 13
    else
      puts "ERROR: Unknown card face value: #{face_value}, should be A,2-10,J,Q,K"
    end
    return val
  end
end

class Deck < Array
  def initialize
    ["C", "D", "H", "S"].each do |suit|
      ["A", 2, 3, 4, 5, 6, 7, 8, 9, 10, "J", "Q", "K"].each do |face_value|
	self.push(Card.new(face_value, suit))
      end
    end
    self.push(Card.new("AJoker", nil))
    self.push(Card.new("BJoker", nil))
    @deck_size = self.size
  end

  def dump
    self.each do |c|
      if (c.value == 53)
        print c.face_value
      else
	print c.value
      end
      print " "
    end
    print "\n\n"
    if (@deck_size != self.size) then
      puts "ERROR!  Deck size changed to #{self.size}"
      exit
    end
  end

  def find_joker(j)
    self.each_index do |i|
      if (self[i].face_value == j)
	return i
      end
    end
    puts "ERROR: Could not find joker '#{j}' in deck."
  end

  def move_card_down(pos, num)
    print "before move_card_down(#{pos}, #{num}): " if $debug
    self.dump if $debug
    dest = pos + num
    dest -= (self.size-1) if (dest >= self.size)
    card = self.delete_at(pos)
    temp = self.dup
    self.clear
    temp.slice(0, dest).each {|x| self.push(x) }
    self << card
    temp.slice(dest..(-1)).each {|x| self.push(x) }
    print "after move_card_down(#{pos}, #{num}): " if $debug
    self.dump if $debug
  end

  def triple_cut_split(a, b)
    a,b=b,a if (a > b)
    print "before triple_cut_split(#{a}, #{b}): " if $debug
    self.dump if $debug
    temp = self.dup
    self.clear
    temp.slice((b+1)..-1).each {|x| self.push(x) }
    temp.slice(a..b).each {|x| self.push(x) }
    temp.slice(0..(a-1)).each {|x| self.push(x) }
    print "after triple_cut_split(#{a}, #{b}): " if $debug
    self.dump if $debug
  end

  def count_cut
    print "before count_cut: " if $debug
    self.dump if $debug
    temp = self.dup
    self.clear
    num = temp[-1].value
    temp.slice(num..-2).each {|x| self.push(x) }
    temp.slice(0..(num-1)).each {|x| self.push(x) }
    self.push(temp[-1])
    print "after count_cut: " if $debug
    self.dump if $debug
  end

  def output_letter
    num = self[0].value
    card = self[num]
    return nil if (card.value == 53)
    num = (card.value > 26 ? card.value-26 : card.value)
    char = (num-1 + "A"[0]).chr
    puts "card.value=#{card.value}, char=#{char}" if $debug
    return char
  end

  def keystream_message(msg)
    # result = "DWJXHYRFDGTMSHPUURXJ"
    result = ""
    while (result.length < msg.length) do
      # Step 2 - Move the A Joker down one card
      pos = find_joker("AJoker")
      move_card_down(pos, 1)
      # Step 3 - Move the B Joker down two cards
      pos = find_joker("BJoker")
      move_card_down(pos, 2)
      # Step 4 - Triple cut split around two jokers
      apos = find_joker("AJoker")
      bpos = find_joker("BJoker")
      triple_cut_split(apos, bpos)
      # Step 5 - Count cut
      count_cut
      # Step 6 - Output letter - might be nil
      letter = output_letter
      result << letter if letter
    end
    return result
  end
end

message = ARGV[0].dup

encrypted = true
encrypted = false if (message =~ /[a-z]/)
words = message.split(/\s+/)
words.each do |word|
  encrypted = false if (word.length != 5)
  encrypted = false if (word =~ /[^A-Z]/)
end

def message2nums(msg)
  result = []
  msg.each_byte do |c|
    result.push(c+1-"A"[0])
  end
  return result
end

def nums2message(nums)
  result = ""
  nums.each do |val|
    result << (val-1+"A"[0]).chr
  end
  return result
end

deck = Deck.new

if encrypted then
  puts "Encrypted message: '#{message}'"
  message.gsub!(/[^A-Z]/, '')

  # Step 1
  keystream_message = deck.keystream_message(message)
  # puts "keystream_message = #{keystream_message}"

  # Step 2
  num_message = message2nums(message)
  # puts "num_message = "
  # p num_message

  # Step 3
  num_keystream = message2nums(keystream_message)
  # puts "num_keystream = "
  # p num_keystream

  # Step 4
  num_result = []
  num_message.each_index do |index|
    num_result[index] = num_message[index] - num_keystream[index]
    num_result[index] += 26 if (num_result[index] < 1)
  end

  # Step 6
  result = nums2message(num_result)
  print "Unencrypted message: "
  count = 0
  result.each_byte do |c|
    print c.chr
    count += 1
    print " " if ((count % 5) == 0)
  end
  print "\n"

else
  puts "Unencrypted message: '#{message}'"

  # Step 1
  message.upcase!
  message.gsub!(/[^A-Z]/, '')
  message << "X" * ((message.length % 5)==0 ? 0 : (5-(message.length % 5)))
  # puts "message: #{message}"

  # Step 2
  keystream_message = deck.keystream_message(message)
  # puts "keystream_message = #{keystream_message}"

  # Step 3
  num_message = message2nums(message)
  # puts "num_message = "
  # p num_message

  # Step 4
  num_keystream = message2nums(keystream_message)
  # puts "num_keystream = "
  # p num_keystream

  # Step 5
  num_result = []
  num_message.each_index do |index|
    num_result[index] = num_message[index] + num_keystream[index]
    num_result[index] -= 26 if (num_result[index] > 26)
  end

  # Step 6
  result = nums2message(num_result)
  print "Encrypted message: "
  count = 0
  result.each_byte do |c|
    print c.chr
    count += 1
    print " " if ((count % 5) == 0)
  end
  print "\n"
end
