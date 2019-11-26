package com.tradisys.commons.waves.itest;

import com.tradisys.commons.waves.itest.scm.HardDriveFileExtractor;
import com.tradisys.commons.waves.itest.scm.ScmFileMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class HardDriveFileExtractorTest {

    @Test
    public void testRead() throws Exception {
        testFileRead("/Users/amurashko/Work/Tradisys/GitLab/waves-itest/src/test/resources");
        testFileRead("/Users/amurashko/Work/Tradisys/GitLab/waves-itest/src/test/resources/");
    }

    private void testFileRead(String dirPath) throws IOException {
        HardDriveFileExtractor extractor = new HardDriveFileExtractor(dirPath);
        ScmFileMeta meta = () -> "itest.properties";
        String content = extractor.read(meta);

        Assertions.assertTrue(content.length() > 1);
    }
}
