-- Chat Service Database Schema
-- PostgreSQL 15+

-- FAQ Categories Table
CREATE TABLE IF NOT EXISTS faq_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE faq_category IS 'FAQ分类表';
COMMENT ON COLUMN faq_category.name IS '分类名称';
COMMENT ON COLUMN faq_category.code IS '分类编码';
COMMENT ON COLUMN faq_category.description IS '分类描述';
COMMENT ON COLUMN faq_category.icon IS '分类图标';
COMMENT ON COLUMN faq_category.sort_order IS '排序顺序';
COMMENT ON COLUMN faq_category.status IS '状态: 0-禁用, 1-启用';

-- FAQ Knowledge Base Table
CREATE TABLE IF NOT EXISTS faq_knowledge (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES faq_category(id),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    keywords TEXT[],
    intent_tags TEXT[],
    priority INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    helpful_count INTEGER DEFAULT 0,
    unhelpful_count INTEGER DEFAULT 0,
    status SMALLINT DEFAULT 1,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE faq_knowledge IS 'FAQ知识库表';
COMMENT ON COLUMN faq_knowledge.category_id IS '分类ID';
COMMENT ON COLUMN faq_knowledge.question IS '问题';
COMMENT ON COLUMN faq_knowledge.answer IS '答案(支持HTML格式)';
COMMENT ON COLUMN faq_knowledge.keywords IS '关键词数组';
COMMENT ON COLUMN faq_knowledge.intent_tags IS '意图标签数组';
COMMENT ON COLUMN faq_knowledge.priority IS '优先级(越高越优先匹配)';
COMMENT ON COLUMN faq_knowledge.view_count IS '查看次数';
COMMENT ON COLUMN faq_knowledge.helpful_count IS '有帮助次数';
COMMENT ON COLUMN faq_knowledge.unhelpful_count IS '无帮助次数';
COMMENT ON COLUMN faq_knowledge.status IS '状态: 0-禁用, 1-启用';

-- Create GIN index for keyword/intent search
CREATE INDEX IF NOT EXISTS idx_faq_knowledge_keywords ON faq_knowledge USING GIN(keywords);
CREATE INDEX IF NOT EXISTS idx_faq_knowledge_intent_tags ON faq_knowledge USING GIN(intent_tags);
CREATE INDEX IF NOT EXISTS idx_faq_knowledge_category ON faq_knowledge(category_id);

-- Intent Definition Table
CREATE TABLE IF NOT EXISTS chat_intent (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    patterns TEXT[],
    entities TEXT[],
    response_template TEXT,
    action_type VARCHAR(50),
    action_params JSONB,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

COMMENT ON TABLE chat_intent IS '意图定义表';
COMMENT ON COLUMN chat_intent.name IS '意图名称';
COMMENT ON COLUMN chat_intent.code IS '意图编码';
COMMENT ON COLUMN chat_intent.patterns IS '匹配模式数组';
COMMENT ON COLUMN chat_intent.entities IS '关联实体数组';
COMMENT ON COLUMN chat_intent.response_template IS '响应模板';
COMMENT ON COLUMN chat_intent.action_type IS '动作类型: FAQ_LOOKUP, API_CALL, TRANSFER, NONE';
COMMENT ON COLUMN chat_intent.action_params IS '动作参数(JSON)';

CREATE INDEX IF NOT EXISTS idx_chat_intent_patterns ON chat_intent USING GIN(patterns);

-- Chat Session Table
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    reader_id BIGINT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    resolved BOOLEAN DEFAULT FALSE,
    satisfaction_score INTEGER,
    feedback TEXT,
    context JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE chat_session IS '会话记录表';
COMMENT ON COLUMN chat_session.session_id IS '会话唯一ID';
COMMENT ON COLUMN chat_session.reader_id IS '读者ID(可为空-匿名用户)';
COMMENT ON COLUMN chat_session.message_count IS '消息数量';
COMMENT ON COLUMN chat_session.resolved IS '是否已解决';
COMMENT ON COLUMN chat_session.satisfaction_score IS '满意度评分(1-5)';
COMMENT ON COLUMN chat_session.context IS '会话上下文(JSON)';

CREATE INDEX IF NOT EXISTS idx_chat_session_reader ON chat_session(reader_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_time ON chat_session(start_time);

-- Chat Message Table
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    intent_code VARCHAR(50),
    confidence DECIMAL(5,4),
    matched_faq_id BIGINT REFERENCES faq_knowledge(id),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE chat_message IS '会话消息表';
COMMENT ON COLUMN chat_message.session_id IS '会话ID';
COMMENT ON COLUMN chat_message.role IS '角色: user, assistant, system';
COMMENT ON COLUMN chat_message.content IS '消息内容';
COMMENT ON COLUMN chat_message.intent_code IS '识别的意图编码';
COMMENT ON COLUMN chat_message.confidence IS '意图置信度(0-1)';
COMMENT ON COLUMN chat_message.matched_faq_id IS '匹配的FAQ知识ID';
COMMENT ON COLUMN chat_message.metadata IS '元数据(JSON)';

CREATE INDEX IF NOT EXISTS idx_chat_message_session ON chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_time ON chat_message(created_at);

-- Chat Feedback Table
CREATE TABLE IF NOT EXISTS chat_feedback (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    message_id BIGINT REFERENCES chat_message(id),
    faq_id BIGINT REFERENCES faq_knowledge(id),
    feedback_type VARCHAR(20) NOT NULL,
    comment TEXT,
    reader_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE chat_feedback IS '会话反馈表';
COMMENT ON COLUMN chat_feedback.feedback_type IS '反馈类型: helpful, unhelpful, report';
COMMENT ON COLUMN chat_feedback.comment IS '反馈评论';

CREATE INDEX IF NOT EXISTS idx_chat_feedback_session ON chat_feedback(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_feedback_faq ON chat_feedback(faq_id);

-- Hot Questions Statistics Table
CREATE TABLE IF NOT EXISTS hot_question_stats (
    id BIGSERIAL PRIMARY KEY,
    question_text TEXT NOT NULL,
    normalized_text TEXT NOT NULL,
    faq_id BIGINT REFERENCES faq_knowledge(id),
    ask_count INTEGER DEFAULT 1,
    last_asked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE hot_question_stats IS '热门问题统计表';
COMMENT ON COLUMN hot_question_stats.normalized_text IS '标准化后的问题文本';
COMMENT ON COLUMN hot_question_stats.ask_count IS '提问次数';

CREATE INDEX IF NOT EXISTS idx_hot_question_count ON hot_question_stats(ask_count DESC);
CREATE INDEX IF NOT EXISTS idx_hot_question_normalized ON hot_question_stats(normalized_text);
