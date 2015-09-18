/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.service;

import com.thoughtworks.go.GoConfigRevisions;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.exceptions.ConfigMergeException;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static java.lang.String.format;

/**
 * @understands versioning cruise-config
 */
@Component
public class ConfigRepository {
    private static final String CRUISE_CONFIG_XML = "cruise-config.xml";
    private static final String STUDIOS_PRODUCT = "support@thoughtworks.com";
    static final String BRANCH_AT_REVISION = "branch-at-revision";
    static final String BRANCH_AT_HEAD = "branch-at-head";
    public static final String CURRENT = "current";

    private File workingDir;

    private static final Logger LOGGER = Logger.getLogger(ConfigRepository.class);
    private Git git;
    private Repository gitRepo;

    @Autowired
    public ConfigRepository(SystemEnvironment systemEnvironment) throws IOException {
        workingDir = systemEnvironment.getConfigRepoDir();
        File configRepoDir = new File(workingDir, ".git");
        gitRepo = new FileRepository(configRepoDir);
        git = new Git(gitRepo);
    }

    public void initialize() throws IOException {
        File configRepoDir = new File(workingDir, ".git");
        if (!configRepoDir.exists()) {
            gitRepo.create();
        }
        else {
            cleanAndResetToMaster();
        }
    }

    @Deprecated // used in test only
    Git git() {
        return git;
    }

    public boolean isRepositoryCorrupted() {
        boolean result = false;
        try {
            git.status().call();
        } catch (Exception e) {
            result = true;
        }
        return result;
    }

    public void checkin(final GoConfigRevision rev) throws Exception {
        try {
            if (rev.equals(getCurrentRevision())) {
                return;
            }
            final File file = new File(workingDir, CRUISE_CONFIG_XML);
            FileUtil.writeContentToFile(rev.getContent(), file);
            final AddCommand addCommand = git.add();
            doLocked(new VoidThrowingFn<Exception>() {
                public void run() throws Exception {
                    addCommand.addFilepattern(CRUISE_CONFIG_XML).call();
                    git.commit().setAuthor(rev.getUsername(), STUDIOS_PRODUCT).setMessage(rev.getComment()).call();
                }
            });
        } catch (Exception e) {
            LOGGER.error(format("[CONFIG SAVE] Check-in failed for %s", rev.toString()), e);
            throw e;
        }
    }

    public <T, E extends Exception> T doLocked(ThrowingFn<T, E> runnable) throws E {
        synchronized (this) {
            return runnable.call();
        }
    }

    public GoConfigRevision getRevision(String md5) throws GitAPIException {
        return CURRENT.equals(md5) ? getCurrentRevision() : findRevisionByMd5(md5);
    }

    Iterable<RevCommit> revisions() throws GitAPIException {
        LogCommand command = git.log();
        return command.call();
    }

    private GoConfigRevision findRevisionByMd5(final String md5) throws GitAPIException {
        return doLocked(new ThrowingFn<GoConfigRevision, GitAPIException>() {
            public GoConfigRevision call() throws GitAPIException {
                return getGoConfigRevision(getRevCommitForMd5(md5));
            }
        });
    }

    public RevCommit getRevCommitForMd5(String md5) throws GitAPIException {
        if(md5 == null)
            throw new NullArgumentException("md5");

        final String expectedPart = GoConfigRevision.Fragment.md5.represent(GoConfigRevision.esc(md5));
        for (RevCommit revision : revisions()) {
            String message = revision.getFullMessage();
            if (message.endsWith(expectedPart)) {
                return revision;
            }
        }
        throw new IllegalArgumentException(String.format("There is no config version corresponding to md5: '%s'", md5));
    }

	RevCommit getRevCommitForCommitSHA(String commitSHA) throws GitAPIException {
		for (RevCommit revision : revisions()) {
			if (revision.getName().equals(commitSHA)) {
				return revision;
			}
		}
		throw new IllegalArgumentException(String.format("There is no commit corresponding to SHA: '%s'", commitSHA));
	}

    public GoConfigRevision getCurrentRevision() {
        return doLocked(new ThrowingFn<GoConfigRevision, RuntimeException>() {
            public GoConfigRevision call() {
                RevCommit revision;
                try {
                    revision = getCurrentRevCommit();
                } catch (GitAPIException e) {
                    LOGGER.info("[CONFIG REPOSITORY] Unable retrieve current cruise config revision", e);
                    return null;
                }
                return getGoConfigRevision(revision);
            }
        });

    }

    public RevCommit getCurrentRevCommit() throws GitAPIException {
        try {
            return revisions().iterator().next();
        } catch (GitAPIException e) {
            LOGGER.error("[CONFIG REPOSITORY] Could not fetch latest commit id", e);
            throw e;
        }
    }

	public GoConfigRevisions getCommits(final int pageSize, final int offset) throws Exception {
		return doLocked(new ThrowingFn<GoConfigRevisions, RuntimeException>() {
			public GoConfigRevisions call() {
				GoConfigRevisions goConfigRevisions = new GoConfigRevisions();
				try {
					LogCommand command = git.log().setMaxCount(pageSize).setSkip(offset);
					Iterable<RevCommit> revisions = command.call();
					for (RevCommit revision : revisions) {
						GoConfigRevision goConfigRevision = new GoConfigRevision(null, revision.getFullMessage());
						goConfigRevision.setCommitSHA(revision.name());
						goConfigRevisions.add(goConfigRevision);
					}
				} catch (Exception e) {
					// ignore
				}
				return goConfigRevisions;
			}
		});
	}

    private GoConfigRevision getGoConfigRevision(final RevCommit revision) {
        return new GoConfigRevision(contentFromTree(revision.getTree()), revision.getFullMessage());
    }

    private String contentFromTree(RevTree tree) {
        try {
            final ObjectReader reader = gitRepo.newObjectReader();
            CanonicalTreeParser parser = new CanonicalTreeParser();
            parser.reset(reader, tree);

            String lastPath = null;
            while (true) {
                final String path = parser.getEntryPathString();
                parser = parser.next();
                if (path.equals(lastPath)) {
                    break;
                }

                lastPath = path;

                if (path.equals(CRUISE_CONFIG_XML)) {
                    final ObjectId id = parser.getEntryObjectId();
                    final ObjectLoader loader = reader.open(id);
                    return new String(loader.getBytes());
                }
            }
            return null;
        } catch (IOException e) {
            LOGGER.error(String.format("Could not fetch content from the config repository found at path '%s'", workingDir.getAbsolutePath()), e);
            throw new RuntimeException("Error while fetching content from the config repository.", e);
        }
    }

    public String configChangesFor(final String laterMD5, final String earlierMD5) throws GitAPIException {
        return doLocked(new ThrowingFn<String, GitAPIException>() {
            public String call() throws GitAPIException {
                RevCommit laterCommit = null;
                RevCommit earlierCommit = null;
                if(!StringUtil.isBlank(laterMD5)) {
                    laterCommit = getRevCommitForMd5(laterMD5);
                }
                if(!StringUtil.isBlank(earlierMD5))
                    earlierCommit = getRevCommitForMd5(earlierMD5);
                return findDiffBetweenTwoRevisions(laterCommit, earlierCommit);
            }
        });
    }

	public String configChangesForCommits(final String fromRevision, final String toRevision) throws GitAPIException {
		return doLocked(new ThrowingFn<String, GitAPIException>() {
			public String call() throws GitAPIException {
				RevCommit laterCommit = null;
				RevCommit earlierCommit = null;
				if (!StringUtil.isBlank(fromRevision)) {
					laterCommit = getRevCommitForCommitSHA(fromRevision);
				}
				if (!StringUtil.isBlank(toRevision)) {
					earlierCommit = getRevCommitForCommitSHA(toRevision);
				}
				return findDiffBetweenTwoRevisions(laterCommit, earlierCommit);
			}
		});
	}

    String findDiffBetweenTwoRevisions(RevCommit laterCommit, RevCommit earlierCommit) throws GitAPIException {
        if (laterCommit == null || earlierCommit == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String output = null;
        try {
            DiffFormatter diffFormatter = new DiffFormatter(out);
            diffFormatter.setRepository(gitRepo);
            diffFormatter.format(earlierCommit.getId(), laterCommit.getId());
            output = out.toString();
            output = StringUtil.stripTillLastOccurrenceOf(output, "+++ b/cruise-config.xml");
        } catch (IOException e) {
            throw new RuntimeException("Error occurred during diff computation. Message: " + e.getMessage());
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
        return output;
    }

    RevCommit getPreviousCommit(RevCommit givenCommit) throws GitAPIException {
        Iterable<RevCommit> revisions = revisions();
        Iterator<RevCommit> iterator = revisions.iterator();

        RevCommit current, next = null;
        while (iterator.hasNext()) {
            current = iterator.next();
            if (current.equals(givenCommit)) {
                if (iterator.hasNext()) {
                    next = iterator.next();
                    break;
                }
            }
        }
        return next;
    }

    public String getConfigMergedWithLatestRevision(GoConfigRevision configRevision, String oldMD5) throws Exception {
        try {
            LOGGER.debug("[Config Save] Starting git merge of config");
            createBranch(BRANCH_AT_REVISION, getRevCommitForMd5(oldMD5));
            createBranch(BRANCH_AT_HEAD, getCurrentRevCommit());
            RevCommit newCommit = checkinToBranch(BRANCH_AT_REVISION, configRevision);
            return getMergedConfig(BRANCH_AT_HEAD, newCommit);
        } catch (Exception e) {
            LOGGER.info("[CONFIG_MERGE] Could not merge");
            throw new ConfigMergeException(e.getMessage(), e);
        } finally {
            cleanAndResetToMaster();
            LOGGER.debug("[Config Save] Ending git merge of config");
        }
    }

    void createBranch(String branchName, RevCommit revCommit) throws GitAPIException {
        try {
            git.branchCreate().setName(branchName).setStartPoint(revCommit).call();
        } catch (GitAPIException e) {
            LOGGER.error(String.format("[CONFIG_MERGE] Failed to create branch %s at revision %s", branchName, revCommit.getId()), e);
            throw e;
        }
    }

    void deleteBranch(String branchName) throws GitAPIException {
        try {
            git.branchDelete().setBranchNames(branchName).setForce(true).call();
        } catch (GitAPIException e) {
            LOGGER.error(String.format("[CONFIG_MERGE] Failed to delete branch %s", branchName), e);
            throw e;
        }
    }

    RevCommit checkinToBranch(String branchName, GoConfigRevision rev) throws Exception {
        try {
            checkout(branchName);
            checkin(rev);
            return getCurrentRevCommit();
        } catch (Exception e) {
            LOGGER.error(String.format("[CONFIG_MERGE] Check-in to branch %s failed", branchName), e);
            throw e;
        }
    }

    String getMergedConfig(String branchName, RevCommit newCommit) throws GitAPIException, IOException {
        MergeResult result = null;
        try {
            checkout(branchName);
            result = git.merge().include(newCommit).call();
        } catch (GitAPIException e) {
            LOGGER.info(String.format("[CONFIG_MERGE] Merging commit %s by user %s to branch %s at revision %s failed", newCommit.getId().getName(), newCommit.getAuthorIdent().getName(), branchName, getCurrentRevCommit().getId().getName()));
            throw e;
        }
        if (!result.getMergeStatus().isSuccessful()) {
            LOGGER.info(String.format("[CONFIG_MERGE] Merging commit %s by user %s to branch %s at revision %s failed as config file has changed", newCommit.getId().getName(), newCommit.getAuthorIdent().getName(), branchName,
                    getCurrentRevCommit().getId().getName()));
            throw new ConfigFileHasChangedException();
        }
        LOGGER.info(String.format("[CONFIG_MERGE] Successfully merged commit %s by user %s to branch %s. Merge commit revision is %s", newCommit.getId().getName(), newCommit.getAuthorIdent().getName(), branchName, getCurrentRevCommit().getId().getName()));
        return FileUtils.readFileToString(new File(workingDir, CRUISE_CONFIG_XML));
    }

    private void checkout(String branchName) throws GitAPIException {
        try {
            git.checkout().setName(branchName).call();
        } catch (GitAPIException e) {
            LOGGER.error(format("[CONFIG_MERGE] Checkout to branch %s failed", branchName), e);
            throw e;
        }
    }

    void cleanAndResetToMaster() throws IOException {
        try {
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            checkout("master");
            deleteBranch(BRANCH_AT_REVISION);
            deleteBranch(BRANCH_AT_HEAD);
        } catch (Exception e) {
            String currentBranch = git.getRepository().getBranch();
            LOGGER.error(format("Error while trying to clean up config repository, CurrentBranch: %s \n : \n Message: %s", currentBranch, e.getMessage(), e.getStackTrace()), e);
            throw new RuntimeException(e);
        }
    }
}
