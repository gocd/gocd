module ApiV1
  class NotificationFiltersRepresenter < ApiV1::BaseRepresenter

    FilterStruct = Struct.new(:id, :pipelineName, :stageName, :event, :myCheckin)

    def initialize(filters)
      @represented = Struct.new(:filters).new filters.to_a.map { |nf| FilterStruct.new(*Hash(nf.toMap).symbolize_keys.values_at(*FilterStruct.members)) }
    end

    collection :filters do
      property :id
      property :pipelineName, as: :pipeline
      property :stageName, as: :stage
      property :event
      property :myCheckin, as: :match_commits
    end

  end
end
