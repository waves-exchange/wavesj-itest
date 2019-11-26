package com.tradisys.commons.waves.itest.scm;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class HardDriveFileExtractor extends GitHubFileExtractor {

    private File dir;

    public HardDriveFileExtractor(String dirPath) {
        super("empty");
        this.dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid dir path: " + dirPath);
        }
    }

    @Override
    protected String downloadScript(String branch, ScmFileMeta scmFileMeta) throws IOException {
        Path filePath = dir.toPath().resolve(scmFileMeta.getFileName());
        return FileUtils.readFileToString(filePath.toFile());
    }
}
