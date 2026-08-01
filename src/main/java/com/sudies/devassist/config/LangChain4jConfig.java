package com.sudies.devassist.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * LangChain4j 配置（第八刀）。
 * <p>LLM 用 DeepSeek（OpenAI 兼容）；embedding 用本地 bge-small-zh-v1.5（512 维，离线，因 DeepSeek 无 embedding API）；
 * 向量库用 Qdrant（gRPC 6334）。不用 langchain4j-spring-boot-starter 以避免与 SB4 版本耦合，全部手动 @Bean。
 * collection 首次启动幂等创建（512 维 Cosine）。
 */
@Configuration
public class LangChain4jConfig {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${qdrant.host:127.0.0.1}")
    private String host;

    @Value("${qdrant.grpc-port:6334}")
    private int grpcPort;

    @Value("${qdrant.rest-port:6333}")
    private int restPort;

    @Value("${qdrant.collection:dev_assist_chunks}")
    private String collection;

    /**
     * 本地中文 embedding（bge-small-zh-v1.5，512 维，进程内 ONNX 运行，离线）。
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel();
    }

    /**
     * DeepSeek chat（OpenAI 兼容，deepseek-chat 即 V3）。
     */
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return QdrantEmbeddingStore.builder()
                .host(host)
                .port(grpcPort)
                .collectionName(collection)
                .build();
    }

    /**
     * 幂等创建 Qdrant collection（512 维 Cosine；已存在则 Qdrant 返回 409，忽略）。
     */
    @Bean
    public ApplicationRunner qdrantCollectionInitializer() {
        return args -> {
            RestClient client = RestClient.builder().baseUrl("http://" + host + ":" + restPort).build();
            String body = "{\"vectors\":{\"size\":512,\"distance\":\"Cosine\"}}";
            try {
                client.put().uri("/collections/{c}", collection).body(body).retrieve().toBodilessEntity();
            } catch (Exception ignored) {
                // collection 已存在(409) 或 Qdrant 未就绪，忽略；首次写入前会重试
            }
        };
    }
}
