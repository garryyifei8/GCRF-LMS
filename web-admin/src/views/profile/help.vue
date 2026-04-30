<template>
  <div class="help-page">
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon><QuestionFilled /></el-icon>
          <span>帮助中心</span>
        </div>
      </template>

      <!-- 系统介绍 -->
      <section class="help-section">
        <h2 class="section-title">系统介绍</h2>
        <el-card shadow="never" class="intro-card">
          <p class="intro-text">
            国创睿峰智能图书馆管理系统（GCRF Library Management
            System）是一套面向高校及公共图书馆的一体化数字化管理平台。
            系统涵盖图书管理、流通管理、读者管理、AI
            智能推荐、智能问答等核心功能，助力图书馆实现数字化转型与精细化运营。
          </p>
          <el-row :gutter="20" class="feature-grid">
            <el-col :span="8" v-for="f in features" :key="f.title">
              <div class="feature-item">
                <el-icon class="feature-icon" :style="{ color: f.color }">
                  <component :is="f.icon" />
                </el-icon>
                <div class="feature-title">{{ f.title }}</div>
                <div class="feature-desc">{{ f.desc }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </section>

      <!-- 常见问题 -->
      <section class="help-section">
        <h2 class="section-title">常见问题（FAQ）</h2>
        <el-collapse accordion class="faq-collapse">
          <el-collapse-item v-for="(item, index) in faqs" :key="index" :name="index">
            <template #title>
              <div class="faq-question">
                <span class="faq-index">Q{{ index + 1 }}</span>
                {{ item.question }}
              </div>
            </template>
            <div class="faq-answer">{{ item.answer }}</div>
          </el-collapse-item>
        </el-collapse>
      </section>

      <!-- 联系方式 -->
      <section class="help-section">
        <h2 class="section-title">联系方式</h2>
        <el-card shadow="never" class="contact-card">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="技术支持邮箱">
              <el-link type="primary" href="mailto:support@gcrf-library.com"
                >support@gcrf-library.com</el-link
              >
            </el-descriptions-item>
            <el-descriptions-item label="客服热线">400-888-9999</el-descriptions-item>
            <el-descriptions-item label="服务时间">周一至周五 09:00 - 18:00</el-descriptions-item>
            <el-descriptions-item label="官方网站">
              <el-link type="primary" href="https://www.gcrf-library.com" target="_blank"
                >www.gcrf-library.com</el-link
              >
            </el-descriptions-item>
          </el-descriptions>
          <el-alert
            title="如遇紧急问题，请直接拨打客服热线，我们将在第一时间为您处理。"
            type="info"
            :closable="false"
            show-icon
            class="contact-alert"
          />
        </el-card>
      </section>
    </el-card>
  </div>
</template>

<script setup>
import { QuestionFilled, Reading, User, MagicStick, Setting, Coin } from '@element-plus/icons-vue'

const features = [
  { title: '图书管理', desc: '图书编目、典藏、盘点全流程管理', icon: 'Reading', color: '#5D87FF' },
  { title: '流通管理', desc: '借还书、预约、催还一体化处理', icon: 'Coin', color: '#49BEFF' },
  { title: '读者管理', desc: '学生、教师读者证件统一管理', icon: 'User', color: '#13DEB9' },
  {
    title: 'AI 智能推荐',
    desc: '基于行为分析的个性化书目推荐',
    icon: 'MagicStick',
    color: '#FFAE1F'
  },
  {
    title: '智能问答',
    desc: '自然语言交互，快速解答馆藏咨询',
    icon: 'MagicStick',
    color: '#FA896B'
  },
  { title: '系统管理', desc: '用户、角色、权限灵活配置', icon: 'Setting', color: '#764BA2' }
]

const faqs = [
  {
    question: '如何重置账号密码？',
    answer:
      '请前往「个人中心 → 修改密码」页面，输入当前密码及新密码后提交即可完成修改。若忘记当前密码，请联系系统管理员在「系统管理 → 用户管理」中为您重置密码。'
  },
  {
    question: '图书借阅期限是多少？逾期如何处理？',
    answer:
      '普通图书借阅期限为 30 天，可续借一次，续借后最长 60 天。逾期归还将按天计算滞纳金，具体标准以图书馆规定为准。系统会在到期前 3 天发送提醒通知。'
  },
  {
    question: '读者证丢失了怎么办？',
    answer:
      '请携带本人有效证件前往图书馆服务台申请补办，工作人员将在「读者管理 → 读者证办理」中为您重新办理。补办期间可凭有效证件借阅。'
  },
  {
    question: 'AI 推荐功能不准确，如何改善？',
    answer:
      'AI 推荐基于您的借阅历史和浏览记录进行学习。新账号推荐需要一定数据积累，建议您在「AI → 智能推荐」页面主动操作"感兴趣"或"不感兴趣"来快速训练推荐模型。'
  },
  {
    question: '数据导出功能在哪里？',
    answer:
      '各管理模块的列表页面（如图书列表、流通记录、读者列表）均提供导出按钮，支持导出 Excel 格式。如需批量数据，请前往「系统管理 → 数据备份」页面操作。'
  },
  {
    question: '如何给管理员反馈系统 Bug 或功能建议？',
    answer:
      '请前往「个人中心 → 问题反馈」页面提交您的反馈，支持 Bug 报告、功能建议和其他类型。我们会在 3 个工作日内跟进处理，结果会通过系统消息通知您。'
  }
]
</script>

<style lang="scss" scoped>
.help-page {
  .page-card {
    border-radius: 12px;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #2a3547;

      .el-icon {
        font-size: 20px;
        color: #5d87ff;
      }
    }
  }

  .help-section {
    margin-bottom: 32px;

    &:last-child {
      margin-bottom: 0;
    }

    .section-title {
      font-size: 15px;
      font-weight: 600;
      color: #2a3547;
      margin: 0 0 16px;
      padding-left: 10px;
      border-left: 3px solid #5d87ff;
    }
  }

  .intro-card {
    background: #fafbff;

    .intro-text {
      font-size: 14px;
      color: #5a6a85;
      line-height: 1.8;
      margin: 0 0 20px;
    }

    .feature-grid {
      .feature-item {
        text-align: center;
        padding: 16px 8px;
        border-radius: 8px;
        transition: background 0.2s;

        &:hover {
          background: rgba(93, 135, 255, 0.06);
        }

        .feature-icon {
          font-size: 32px;
          margin-bottom: 8px;
        }

        .feature-title {
          font-size: 14px;
          font-weight: 600;
          color: #2a3547;
          margin-bottom: 4px;
        }

        .feature-desc {
          font-size: 12px;
          color: #a1a5b7;
          line-height: 1.5;
        }
      }
    }
  }

  .faq-collapse {
    :deep(.el-collapse-item__header) {
      font-size: 14px;
      height: auto;
      padding: 14px 0;
    }

    .faq-question {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 500;
      color: #2a3547;

      .faq-index {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 26px;
        height: 26px;
        background: #5d87ff;
        color: #fff;
        border-radius: 6px;
        font-size: 12px;
        font-weight: 700;
        flex-shrink: 0;
      }
    }

    .faq-answer {
      font-size: 13px;
      color: #5a6a85;
      line-height: 1.8;
      padding: 4px 0 4px 36px;
    }
  }

  .contact-card {
    .contact-alert {
      margin-top: 16px;
    }
  }
}
</style>
