package com.sudies.devassist.common.constant;

/**
 * AI 模块相关常量（对应关键流程详细设计 §3、§4）。
 */
public final class AiConst {

    private AiConst() {
    }

    /**
     * SSE 流式结束标记
     */
    public static final String SSE_DONE = "[DONE]";

    /**
     * Prompt 模板目录（classpath）
     */
    public static final String PROMPT_DIR = "prompt/";

    /**
     * Qdrant collection 名
     */
    public static final String QDRANT_COLLECTION = "dev_assist_chunks";

    /**
     * Embedding 维度（DashScope text-embedding-v3）
     */
    public static final int EMBEDDING_DIM = 1024;
}
