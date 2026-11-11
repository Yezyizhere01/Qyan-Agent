package com.ty.infinitechatagent.ai;

import com.ty.infinitechatagent.skill.SkillTool;
import com.ty.infinitechatagent.tool.EmailTool;
import com.ty.infinitechatagent.tool.RagTool;
import com.ty.infinitechatagent.tool.TimeTool;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatService {

    @Resource
    private ChatModel chatModel;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    // ★ 由混合检索改为「混合检索 + rerank 精排」的两阶段检索出口
    @Resource
    @Qualifier("rerankingContentRetriever")
    private ContentRetriever rerankingContentRetriever;

    @Resource
    private RagTool ragTool;

    @Resource
    private EmailTool emailTool;

    @Resource
    private StreamingChatModel streamingChatModel;

    // ★ 新增：Skills 运行时
    @Resource
    private SkillTool skillTool;

    @Bean
    public AiChat aiChat() {

        return AiServices.builder(AiChat.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .contentRetriever(rerankingContentRetriever)

                // ★ L1：把技能清单追加到 @SystemMessage 读到的提示词后面。
                //   transformer 跑在 @SystemMessage 解析之后，所以 AiChat 接口一行都不用改。
                .systemMessageTransformer(sm -> sm + "\n\n" + skillTool.catalog())

                .chatMemoryProvider(memoryId -> MessageWindowChatMemory
                        .builder()
                        .id(memoryId)
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .build())
                // ★ 末尾追加 skillTool，让模型能调 loadSkill
                .tools(new TimeTool(), ragTool, emailTool, skillTool)
                .toolProvider(mcpToolProvider)
                .build();
    }

}