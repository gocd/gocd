package com.thoughtworks.go.domain.materials.svn;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.domain.BuildCommand.*;

public class SvnMaterialUpdater {
    private SvnMaterial material;

    public SvnMaterialUpdater(SvnMaterial material) {
        this.material = material;
    }

    public BuildCommand updateTo(String baseDir, RevisionContext revisionContext) {
        Revision revision = revisionContext.getLatestRevision();
        String workingDir = material.workingdir(new File(baseDir)).getPath();

        BuildCommand shouldCheckout = or(
                test("-nd", workingDir),
                test("-nd", new File(workingDir, ".svn").getPath()),
                isRepositoryChanged(workingDir));

        return compose(
                echoWithPrefix("Start updating %s at revision %s from %s", material.updatingTarget(), revision.getRevision(), material.getUrl()),
                cond(shouldCheckout,
                        freshCheckout(workingDir, revision),
                        cleanupAndUpdate(workingDir, revision)),
                echoWithPrefix("Done.\n")
        );

    }

    private BuildCommand cleanupAndUpdate(String workingDir, Revision revision) {
        return compose(
                svn("cleanup", workingDir),
                svn("revert", "--recursive", workingDir),
                svnWithAuth("update", "--non-interactive", "-r", revision.getRevision(), workingDir));
    }

    private BuildCommand isRepositoryChanged(String workingDir) {
        return test("-ncontains",
                "URL: " + StringUtil.removeTrailingSlash(material.getUrl()) + "\n",
                svn("info", "--non-interactive", workingDir));
    }

    private BuildCommand freshCheckout(String workingDir, Revision revision) {
        return compose(
                cond(test("-d", workingDir),
                        cleandir(workingDir),
                        mkdirs(workingDir)),
                svnWithAuth("checkout", "--non-interactive", "-r", revision.getRevision(), material.getUrl(), workingDir));
    }

    private BuildCommand svnWithAuth(String... args) {
        List<String> authArgs = new ArrayList<>();
        List<BuildCommand> commands = new ArrayList<>();
        if (!StringUtils.isBlank(material.getUserName())) {
            authArgs.add("--username");
            authArgs.add(material.getUserName());

            if (!StringUtils.isBlank(material.getPassword())) {
                authArgs.add("--password");
                authArgs.add(material.getPassword());
                commands.add(secret(material.getPassword()));
            }
        }
        String[] combined = authArgs.toArray(new String[authArgs.size() + args.length]);
        System.arraycopy(args, 0, combined, authArgs.size(), args.length);
        commands.add(svn(combined));
        return compose(commands);
    }

    private BuildCommand svn(String... args) {
        return exec("svn", true, args);
    }
}
