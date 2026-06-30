#!/usr/bin/env ruby
require "rugged"
require "linguist"

repo = Rugged::Repository.new(Dir.pwd)
tree = repo.last_commit.tree

# Mirrors linguist's own include_in_language_stats? logic.
def counted?(blob)
  return false unless blob.language
  return false if blob.vendored? || blob.generated? || blob.documentation?
  d = blob.detectable?                    # attribute override: true / false / nil
  return d unless d.nil?                   # explicit linguist-detectable wins
  %i[programming markup].include?(blob.language.type)   # default rule
end

tree.walk_blobs(:preorder) do |root, entry|
  next if entry[:filemode] == 0120000      # symlinks
  path = root + entry[:name]
  blob = Linguist::LazyBlob.new(repo, entry[:oid], path, entry[:filemode].to_s(8))

  next if counted?(blob)                                        # it's already in the bar — skip
  next if blob.binary? || blob.vendored? || blob.documentation? # assume these are detected correctly
  next if %w[data prose].include? blob.language&.type.to_s      # assume these are detected correctly

  reason =
    if blob.generated?     then "generated"
    elsif blob.language.nil?  then "no language"
    else  "not countable (#{blob.language.type})"   # likely detectable=false
    end

  puts "#{reason}\t#{path}"
end
