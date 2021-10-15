package com.tradisys.commons.waves.itest.scm;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class ClasspathFileExtractor extends GitHubFileExtractor {

    public ClasspathFileExtractor() {
        super("empty", "empty");
    }

    @Override
    protected String downloadScript(String branch, ScmFileMeta scmFileMeta) throws IOException {
        return IOUtils.toString(ClasspathFileExtractor.class.getResourceAsStream(scmFileMeta.getFileName()));
    }
}