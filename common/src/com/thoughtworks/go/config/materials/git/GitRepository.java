package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.domain.materials.svn.MaterialUrl;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.log4j.Logger;

import java.io.File;

public class GitRepository {
    private final String repositoryBranch;
    private File repository;
    private UrlArgument repositoryUrl;
    private boolean shallowClone;
    private static final Logger LOG = Logger.getLogger(GitRepository.class);

    public GitRepository(File repository, UrlArgument repositoryUrl, String branchInRepository, boolean shallowClone) {
        this.repository = repository;
        this.repositoryUrl = repositoryUrl;
        this.repositoryBranch = branchInRepository;
        this.shallowClone = shallowClone;
    }

    public boolean isWorkingCopy() {
        return new File(repository, ".git").isDirectory();
    }


    public boolean isShallow() {
        return new File(repository, ".git/shallow").exists();
    }

    public boolean hasChanged(UrlArgument url, String configBranch) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Current repository url of [" + repository + "]: " + repositoryUrl);
            LOG.trace("Target repository url: " + url);
        }
        return !MaterialUrl.sameUrl(url.forCommandline(), repositoryUrl.forCommandline())
                || !isBranchEqual(configBranch)
                || (!shallowClone && isShallow());
    }

    private boolean isBranchEqual(String configBranch) {
        return configBranch.equals(repositoryBranch);
    }

}

