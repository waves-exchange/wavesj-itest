package com.tradisys.commons.waves.itest.scm;

import java.io.IOException;

public interface ScmFileExtractor {
    String read(ScmFileMeta scmFileMeta) throws IOException;
}