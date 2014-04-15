#--
# Copyright 2006 by Chad Fowler, Rich Kilmer, Jim Weirich and others.
# All rights reserved.
# See LICENSE.txt for permissions.
#++

module Gem

  # Activate the gems specfied by the gem_pairs list.
  #
  # gem_pairs ::
  #   List of gem/version pairs.
  #   Eg.  [['rake', '= 0.8.15'], ['RedCloth', '~> 3.0']]
  # options ::
  #   options[:verbose] => print gems as they are required.
  #
  def self.configure(gem_pairs, options={})
    gem_pairs.each do |name, version|
      require 'rubygems' 
      puts "Activating gem #{name} (version #{version})" if options[:verbose]
      gem name, version
    end
  end
end
