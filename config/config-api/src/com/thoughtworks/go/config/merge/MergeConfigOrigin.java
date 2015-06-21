package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.BaseCollection;

/**
 * @understands configuration has multiple origins
 */
public class MergeConfigOrigin extends BaseCollection<ConfigOrigin> implements ConfigOrigin {

    public MergeConfigOrigin(ConfigOrigin... origins)
    {
        for(ConfigOrigin part : origins)
        {
            this.add(part);
        }
    }

    @Override
    public boolean canEdit() {
        for(ConfigOrigin part : this)
        {
            if(part.canEdit())
                return true;
        }
        return false;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public String displayName() {
        return "TODO merge names";
    }
}
