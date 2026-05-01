package com.lingxi.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI思维导图生成Agent接口
 * 根据用户提示词智能生成结构化的思维导图数据
 *
 * @author lingxi
 */
public interface MindmapAgent {

    /**
     * 根据提示词生成思维导图JSON结构
     * 
     * @param prompt 用户的主题或提示词
     * @param layoutType 布局类型（logical/right/left/map等）
     * @return JSON格式的思维导图结构
     */
    @SystemMessage("""
        你是一位专业的思维导图设计专家，擅长将复杂主题结构化。
        
        请根据用户提供的主题，生成一个完整的思维导图JSON结构。
        
        你必须以严格的JSON格式返回结果，不要包含任何其他文字说明、Markdown标记或代码块符号。
        
        返回格式要求：
        {
          "id": "唯一UUID",
          "topic": "中心主题名称",
          "layout": "{{layoutType}}",
          "children": [
            {
              "id": "唯一UUID",
              "topic": "分支主题",
              "children": [
                {
                  "id": "唯一UUID",
                  "topic": "子节点内容"
                }
              ]
            }
          ]
        }
        
        要求：
        1. id字段必须使用UUID格式（例如：550e8400-e29b-41d4-a716-446655440000）
        2. topic字段内容要简洁明了，不超过20个字
        3. 根节点必须有3-5个主要分支
        4. 每个主要分支下要有2-4个子节点
        5. 内容要与用户主题高度相关，具有逻辑性和层次性
        6. 分支命名要具体、有意义，避免使用"分支1"、"子节点1"等无意义名称
        7. 如果是知识类主题，按知识点分类；如果是计划类主题，按步骤或阶段分类
        8. layout字段必须使用传入的layoutType值：{{layoutType}}
        
        示例（仅供参考结构，实际内容要根据用户主题生成）：
        {
          "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "topic": "Java学习路线",
          "layout": "logical",
          "children": [
            {
              "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
              "topic": "基础语法",
              "children": [
                {
                  "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
                  "topic": "数据类型"
                },
                {
                  "id": "d4e5f6a7-b8c9-0123-defa-234567890123",
                  "topic": "控制流程"
                }
              ]
            },
            {
              "id": "e5f6a7b8-c9d0-1234-efab-345678901234",
              "topic": "面向对象",
              "children": [
                {
                  "id": "f6a7b8c9-d0e1-2345-fabc-456789012345",
                  "topic": "封装继承多态"
                }
              ]
            }
          ]
        }
        """)
    String generateMindmap(@UserMessage String prompt, @V("layoutType") String layoutType);
}
