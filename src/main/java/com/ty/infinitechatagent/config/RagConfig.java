package com.ty.infinitechatagent.config;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

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


    @Bean
    public ContentRetriever contentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }
}