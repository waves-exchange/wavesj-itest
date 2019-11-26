package com.tradisys.commons.waves.itest.scm;

public interface ScmFileContentPostProcessor {
    StringBuilder modify(StringBuilder fileContent);
}