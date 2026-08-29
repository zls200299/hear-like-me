package com.zhs.request;

import java.io.Serializable;

/**
 * 准备内置示例原声请求
 */
public class SampleSourcePrepareReq implements Serializable {

    private String sampleCode;

    public String getSampleCode() {
        return sampleCode;
    }

    public void setSampleCode(String sampleCode) {
        this.sampleCode = sampleCode;
    }
}
