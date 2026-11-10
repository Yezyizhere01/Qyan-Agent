package com.ty.infinitechatagent.config;

import com.ty.infinitechatagent.rag.retriever.HybridContentRetriever;
import com.ty.infinitechatagent.rag.retriever.PgBm25ContentRetriever;
import com.ty.infinitechatagent.rag.splitter.MarkdownHeadingSplitter;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@SuppressWarnings({"all"})
public class RagConfig {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${pgvector.table}")
    private String table;

    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor() {
        DocumentSplitter subSplitter = DocumentSplitters.recursive(1000, 100);


        DocumentSplitter splitter = new MarkdownHeadingSplitter(3, 1500, subSplitter);


        return EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .textSegmentTransformer(textSegment -> {
                    // 获取文件名，如果没有则给默认值
                    String fileName = textSegment.metadata().getString("file_name");
                    String prefix = (fileName != null ? fileName : "Unknown-Source");
                    return TextSegment.from(prefix + "\n" + textSegment.text(), textSegment.metadata());
                })
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }


    /**
     * 稠密检索：向量语义检索(参数与基线一致,方便 A/B 对齐)
     */
    @Bean
    public ContentRetriever vectorContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }

    /**
     * 稀疏检索：基于 ParadeDB 的 BM25 关键词检索
     */
    @Bean
    public PgBm25ContentRetriever bm25ContentRetriever(JdbcTemplate jdbcTemplate) {
        return new PgBm25ContentRetriever(jdbcTemplate, table, 5);
    }

    /**
     * 混合检索：向量 + BM25,RRF 融合后取 top-5。对外暴露给 AiService 和评测接口
     */
    @Bean
    public ContentRetriever hybridContentRetriever(
            @Qualifier("vectorContentRetriever") ContentRetriever vectorContentRetriever,
            PgBm25ContentRetriever bm25ContentRetriever) {
        return new HybridContentRetriever(vectorContentRetriever, bm25ContentRetriever, 5, 60);
    }
}