package com.siyi.imagetransmission.contract.parser;

import com.siyi.imagetransmission.decoder.BaseDecoder;

/**
 * author: shymanzhu
 * data: on 2018/11/4
 */
public abstract class BaseParser {
    /**
     * 解析协议
     * @param protocol 一个完整的协议数据
     */
    public abstract void parse(byte[] protocol);

    public void release() {

    }

    protected BaseDecoder mDecoder;

    public void setDecoder(BaseDecoder decoder) {
        mDecoder = decoder;
    }

    public long getLossCount() {
        return 0;
    }

}
