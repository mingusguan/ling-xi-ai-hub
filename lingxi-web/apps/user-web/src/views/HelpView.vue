<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, nextTick } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

interface Topic {
  id: string;
  label: string;
  /** 一句话说明这个功能解决什么问题 */
  summary: string;
  /** 步骤清单；字符串为纯文本，{ code } 会以代码样式渲染 */
  steps: string[];
  /** 需要注意的边界与限制，如实说明未接入或失败关闭的行为 */
  notes?: string[];
  /** 关联页面，便于一键跳过去操作 */
  route?: { name: string; label: string };
}

interface TopicGroup {
  title: string;
  topics: Topic[];
}

/**
 * 用户使用手册。
 *
 * 内容来源：各页面真实交互（按钮文案、字段、服务端约束），不写未实现的能力；
 * 尚未接入或会失败关闭的功能（对象存储、日历服务商、真实支付）如实标注。
 */
const groups: TopicGroup[] = [
  {
    title: '开始使用',
    topics: [
      {
        id: 'start',
        label: '登录与账号',
        summary: '用手机号登录，首次会自动建号；出生日期决定年龄模式。',
        steps: [
          '在登录页填写手机号（例如 13800000000）并选择出生日期。',
          '点击「登录 / 注册」。已注册过的手机号可以只填手机号，出生日期留空。',
          '登录后进入「今日行动」，左侧是全部功能入口。'
        ],
        notes: [
          '服务端强制 14 周岁以上准入，14 周岁以下会被拒绝注册。',
          '14—17 周岁账号创建后进入「待监护」状态，需监护人接受邀请后才能创建目标、打卡与使用灵犀对话。',
          '当前版本未接入短信验证码，属于过渡实现；对外开放前会补上。'
        ],
        route: { name: 'home', label: '返回今日行动' }
      },
      {
        id: 'guardian-user',
        label: '青少年与监护',
        summary: '青少年发起邀请，成人用邀请令牌接受，监护关系可撤销或提交争议。',
        steps: [
          '青少年：进入「监护」，在「发起监护邀请」处点击「生成邀请令牌」，把令牌交给监护人。',
          '成人：进入「监护」，在「接受监护邀请」处粘贴令牌并点击「接受邀请」。',
          '任何一方可在「关系查询」处输入关系标识，查看状态、撤销关系，或填写理由「提交争议」交由人工处理。'
        ],
        notes: [
          '邀请令牌只展示一次，请及时复制。',
          '监护关系存在未决争议时，接受与撤销会被服务端冻结。'
        ],
        route: { name: 'guardian', label: '前往监护管理' }
      }
    ]
  },
  {
    title: '核心循环',
    topics: [
      {
        id: 'goal-create',
        label: '① 创建目标',
        summary: '先写清楚「做什么」和「怎样算成功」，后面所有行动都挂在目标下。',
        steps: [
          '进入「我的目标」。',
          '在「创建目标」里填写标题（例如「每天阅读 20 分钟」）。',
          '填写成功标准（例如「连续 30 天完成阅读」），点击「创建目标」。',
          '创建出来的是草稿状态，还需要下一步「编排计划」才会真正开始。'
        ],
        notes: [
          '成功标准建议写得能被判断，否则复盘时很难说清有没有达成。'
        ],
        route: { name: 'goals', label: '前往我的目标' }
      },
      {
        id: 'goal-plan',
        label: '② 编排计划并确认',
        summary: '在目标详情里加里程碑与行动，点「直接确认计划」后系统才开始生成每日行动。',
        steps: [
          '在「我的目标」列表点击某个目标，进入目标详情。',
          '「里程碑」区填写标题与阶段成功标准，点「添加里程碑」（可选，但复盘时会按里程碑看进度）。',
          '「行动」区填写行动标题、重复方式（一次性 / 每天 / 每周）、执行时间与开始日期，点「添加行动」。',
          '确认无误后点「直接确认计划」。',
          '若还想改：可先点「仅保存草案」，改完再点「激活已保存草案」。'
        ],
        notes: [
          '确认动作会一次性写入计划版本、里程碑与行动并激活目标，这是一次不可回退的提交，请先核对行动清单。',
          '行动至少要有一条，「直接确认计划」按钮才可用。',
          '行动时间按你所在时区记录，跨时区或夏令时不会串天。'
        ],
        route: { name: 'goals', label: '前往编排计划' }
      },
      {
        id: 'daily',
        label: '③ 每天打卡',
        summary: '在「今日行动」看未来 7 天的行动，逐个标记完成 / 部分完成 / 跳过。',
        steps: [
          '进入「今日行动」，看到「未来 7 天行动」列表，每条包含行动名、日期、时区与状态。',
          '需要留痕时先在「打卡备注」写下执行感受（可选，会记入本次打卡）。',
          '按实际情况点「完成」「部分完成」或「跳过」。',
          '标记后该条变为「已记录」，页面底部会统计已记录与待执行数量。'
        ],
        notes: [
          '「部分完成」不计入连续打卡成就；连续打卡只统计标记为「完成」的记录。',
          '若刚确认计划后这里还是空的，说明该目标的行动开始日期在将来；行动实例是按规则滚动生成的。',
          '打卡结果如果触发了成就，页面顶部会弹出「获得新成就」提示，点「知道了」收起。'
        ],
        route: { name: 'home', label: '前往今日行动' }
      },
      {
        id: 'review',
        label: '④ 周期复盘',
        summary: '系统按周期生成复盘任务，写下结论即完成本次复盘。',
        steps: [
          '进入「周期复盘」，可点「刷新」获取最新列表。',
          '点某条复盘的「查看」，进入复盘详情。',
          '在输入框写下本周期总结与下一周期要调整的地方。',
          '点提交完成复盘。'
        ],
        notes: [
          '结论不能为空。',
          '复盘内容会进入你的隐私数据范围，删除账号时一并按规则处理。'
        ],
        route: { name: 'reviews', label: '前往周期复盘' }
      },
      {
        id: 'achievement',
        label: '成就怎么看',
        summary: '成就由服务端按确定规则授予，不是手动领取的。',
        steps: [
          '进入「我的成就」查看已获得的成就卡片。',
          '可用顶部筛选按类型查看（目标达成 / 里程碑完成 / 连续打卡）。'
        ],
        notes: [
          '三类成就：目标达成、里程碑完成、连续打卡（默认连续 7 天，以服务端配置为准）。',
          '成就在打卡的同一个事务里授予，因此打卡成功即已入账，不需要额外操作。',
          '同一成就不会重复授予。'
        ],
        route: { name: 'achievements', label: '前往我的成就' }
      }
    ]
  },
  {
    title: '灵犀对话',
    topics: [
      {
        id: 'companion',
        label: '和灵犀对话',
        summary: '创建会话 → 描述困惑 → 启动运行 → 对高风险动作确认或拒绝。',
        steps: [
          '进入「灵犀对话」，填写会话标题后点「创建会话」。',
          '在输入框写下你的目标或困惑，点「发送并启动运行」。',
          '运行过程中「执行事件」会实时刷新；涉及高风险的动作会先征求你的同意，点「确认执行」或「拒绝」。',
          '运行结果与长期记忆会显示在下方。'
        ],
        notes: [
          '服务端当前只提供受控 Agent 内核：真实模型与领域工具适配尚未接入，未配置模型时运行会「失败关闭」而不是编造结果。',
          '任何写入类动作都需要你显式确认，不会自动执行。'
        ],
        route: { name: 'companion', label: '打开灵犀对话' }
      },
      {
        id: 'memory',
        label: '记忆管理',
        summary: '查看、更正或删除灵犀对你的长期记忆。',
        steps: [
          '进入「记忆管理」，点「刷新」加载记忆列表。',
          '点某条记忆进入编辑，改完点「保存」；不想保留就删除该条。'
        ],
        notes: [
          '删除记忆会向服务端发起删除传播，聊天中不会再引用被删除的内容。'
        ],
        route: { name: 'memory', label: '前往记忆管理' }
      }
    ]
  },
  {
    title: '多端协同',
    topics: [
      {
        id: 'notification',
        label: '消息与通知偏好',
        summary: '看站内消息、设置免打扰与提醒场景、手动拉取跨端增量。',
        steps: [
          '进入「消息与同步」查看站内消息列表，点某条标记已读。',
          '需要实时接收时点「开始接收」建立实时通道，不需要时点「断开」。',
          '在「通知偏好」填写场景（例如 ACTION_REMINDER）、时区、免打扰时段与渠道，点保存。',
          '换了设备或想强制对齐数据时，在「跨端增量变更」处点「拉取增量」。'
        ],
        notes: [
          '行动提醒会在你设定的执行时间前发出；如果你已经提前打卡，这条提醒会被自动取消，不会补发过期提醒。',
          '通知偏好带乐观锁版本号：两个页面同时改会有一个失败，刷新后重试即可。'
        ],
        route: { name: 'notifications', label: '前往消息与同步' }
      },
      {
        id: 'calendar',
        label: '日历同步',
        summary: '把行动投影到外部日历，撤销时可以选择是否删掉已投影的事件。',
        steps: [
          '进入「日历同步」，在凭据引用处填写服务端凭据引用（不要粘贴明文令牌）。',
          '点「绑定日历」。',
          '点「刷新」查看当前绑定；撤销时选择是否同时删除已生成的事件。'
        ],
        notes: [
          '绑定与撤销都需要近期认证（重新输入一次身份信息）。',
          '真实日历服务商（如 Google / 系统日历）适配尚未接入，当前只完成服务端投影与绑定管理。'
        ],
        route: { name: 'calendar', label: '前往日历同步' }
      }
    ]
  },
  {
    title: '分享与资产',
    topics: [
      {
        id: 'partner',
        label: '伙伴与目标授权',
        summary: '邀请伙伴、按目标授权、发起限时分享链接。伙伴只能看到你授权范围内的进度。',
        steps: [
          '进入「伙伴与分享」，在「邀请伙伴」填入对方的用户标识（userId）并点「发出邀请」。',
          '对方接受后，在「目标授权」选择伙伴关系、要开放的目标与过期时间，勾选允许的动作，点「保存授权」。',
          '在「分享链接」填写目标标识与过期时间生成链接；不再需要时撤销。'
        ],
        notes: [
          '伙伴双方都必须是成人账号。',
          '分享链接必须设置晚于当前时刻的过期时间。',
          '分享链接里的 token 只在创建时返回一次，列表不会回显；请立即复制保存。',
          '授权只覆盖服务端白名单字段：你的私密对话与目标正文默认不会对伙伴可见。'
        ],
        route: { name: 'partner', label: '前往伙伴与分享' }
      },
      {
        id: 'assets',
        label: '内容资产与导入导出',
        summary: '登记私有文件、使用只读模板、发起导入导出任务。',
        steps: [
          '进入「内容资产」，在「登记文件」填写文件名与内容摘要，点「登记为待扫描」。',
          '扫描就绪后可在「我的文件」查看与删除。',
          '「内容模板」由后台审核发布，客户端只读。',
          '在「导入导出」点「创建导出任务」或「创建导入任务」，先在「传输任务」里生成预览，确认后才真正写入。'
        ],
        notes: [
          '服务端未接入私有对象存储适配器时，登记文件与导出会返回 CONTENT_STORAGE_UNAVAILABLE 并失败关闭——这是设计上的安全行为，不会产生假成功。',
          '导出任务有有效期，过期需重新创建。'
        ],
        route: { name: 'assets', label: '前往内容资产' }
      }
    ]
  },
  {
    title: '账号与服务',
    topics: [
      {
        id: 'membership',
        label: '会员与权益',
        summary: '查看商品、订单、订阅与可用权益，可在权益页调整用量。',
        steps: [
          '进入「会员与权益」。',
          '在「可用权益」查看余额与到期时间。',
          '在「商品」查看价格版本，点购买进入下单流程。',
          '在订单与订阅区可查看历史记录；订阅项可申请取消。'
        ],
        notes: [
          '商品目录由运营在后台维护，未上架时列表为空属正常。',
          '真实支付渠道尚未接入，下单与退款目前只完成服务端闭环，不会真实扣款。'
        ],
        route: { name: 'membership', label: '前往会员与权益' }
      },
      {
        id: 'support',
        label: '客服工单',
        summary: '提交工单、查看进度、看客服回复。',
        steps: [
          '进入「客服工单」，填写主题（一句话描述）与详细说明，点「提交工单」。',
          '在列表点某条工单查看对话与处理进度。'
        ],
        notes: [
          '主题限 120 字符；补充复现步骤能显著缩短处理时间。',
          '工单按 SLA 分级处理，涉及支付与安全的会优先。'
        ],
        route: { name: 'support', label: '前往客服工单' }
      },
      {
        id: 'privacy',
        label: '隐私与账号',
        summary: '导出、更正、删除或注销账号，下载导出包，提交年龄申诉。',
        steps: [
          '进入「隐私与账号」，在「隐私权利」选择请求类型（导出 / 更正 / 删除 / 注销）后点「提交请求」。',
          '在「我的请求」用「刷新」查看进度；导出请求完成后可下载导出包。',
          '注销请求有冷静期，冷静期内可以撤销。',
          '对年龄判定有异议时，在「年龄申诉」填写证据引用编号并「提交申诉」。'
        ],
        notes: [
          '导出包是加密的限时链接，过期需重新发起。',
          '注销会按统一逻辑删除规则清除各模块数据，并保留不可抵赖的最小删除审计记录。'
        ],
        route: { name: 'privacy', label: '前往隐私与账号' }
      }
    ]
  }
];

const topics = computed(() => groups.flatMap((group) => group.topics.map((topic) => ({ group: group.title, topic }))));
const activeId = ref<string>('');
const contentEl = ref<HTMLElement | null>(null);
const route = useRoute();

function scrollToTopic(id: string): void {
  const el = document.getElementById(`topic-${id}`);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    activeId.value = id;
  }
}

/** 滚动时高亮左侧当前章节：越靠近视口顶部的章节越"当前"。 */
function handleScroll(): void {
  const container = contentEl.value;
  if (!container) {
    return;
  }
  let bestId = topics.value[0]?.topic.id ?? '';
  let bestDelta = Number.POSITIVE_INFINITY;
  for (const { topic } of topics.value) {
    const el = document.getElementById(`topic-${topic.id}`);
    if (!el) {
      continue;
    }
    const delta = Math.abs(el.getBoundingClientRect().top - 120);
    if (delta < bestDelta) {
      bestDelta = delta;
      bestId = topic.id;
    }
  }
  activeId.value = bestId;
}

onMounted(async () => {
  await nextTick();
  const requested = String(route.query.topic ?? '');
  const initial = topics.value.some((item) => item.topic.id === requested) ? requested : topics.value[0]?.topic.id ?? '';
  activeId.value = initial;
  if (requested) {
    scrollToTopic(requested);
  }
  const scroller = document.querySelector('.shell__content');
  scroller?.addEventListener('scroll', handleScroll, { passive: true });
});

onBeforeUnmount(() => {
  const scroller = document.querySelector('.shell__content');
  scroller?.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="help">
    <!-- 目录 -->
    <aside class="help__toc lx-card">
      <h2 class="help__toc-title">使用手册</h2>
      <p class="lx-muted help__toc-intro">按下面的顺序走一遍，就能把核心流程跑起来。</p>
      <nav>
        <div v-for="group in groups" :key="group.title" class="help__toc-group">
          <p class="help__toc-group-title">{{ group.title }}</p>
          <button
            v-for="topic in group.topics"
            :key="topic.id"
            type="button"
            class="help__toc-item"
            :class="{ 'help__toc-item--active': activeId === topic.id }"
            @click="scrollToTopic(topic.id)"
          >
            {{ topic.label }}
          </button>
        </div>
      </nav>
    </aside>

    <!-- 正文 -->
    <div ref="contentEl" class="help__content">
      <header class="lx-card help__hero">
        <p class="help__eyebrow">GUIDE</p>
        <h1>灵犀伴行使用手册</h1>
        <p class="lx-muted">
          每个功能解决什么问题、具体点哪里、有哪些限制。左侧目录可直接跳转；每节末尾有对应页面的入口。
        </p>
        <div class="help__quick">
          <span class="help__quick-label">新手最短路径</span>
          <ol class="help__quick-steps">
            <li>创建目标 →</li>
            <li>编排计划并确认 →</li>
            <li>每天在「今日行动」打卡 →</li>
            <li>周期结束时写复盘</li>
          </ol>
        </div>
      </header>

      <section
        v-for="entry in topics"
        :id="`topic-${entry.topic.id}`"
        :key="entry.topic.id"
        class="lx-card help__topic"
      >
        <p class="help__topic-group">{{ entry.group }}</p>
        <h2>{{ entry.topic.label }}</h2>
        <p class="help__summary">{{ entry.topic.summary }}</p>

        <h3 class="help__h3">操作步骤</h3>
        <ol class="help__steps">
          <li v-for="(step, index) in entry.topic.steps" :key="index">{{ step }}</li>
        </ol>

        <template v-if="entry.topic.notes && entry.topic.notes.length > 0">
          <h3 class="help__h3">需要注意</h3>
          <ul class="help__notes">
            <li v-for="(note, index) in entry.topic.notes" :key="index">{{ note }}</li>
          </ul>
        </template>

        <RouterLink v-if="entry.topic.route" class="lx-button lx-button--ghost" :to="{ name: entry.topic.route.name }">
          {{ entry.topic.route.label }}
        </RouterLink>
      </section>

      <footer class="lx-card help__footer">
        <strong>还是不清楚？</strong>
        <p class="lx-muted">
          可以先在「客服工单」提交问题，写清你点到了哪一步、期望的结果与实际看到的结果，我们会按 SLA 跟进。
        </p>
        <RouterLink class="lx-button" :to="{ name: 'support' }">前往客服工单</RouterLink>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.help {
  display: grid;
  grid-template-columns: 258px minmax(0, 1fr);
  gap: var(--lx-space-5);
  align-items: start;
}

/* --------------------------------------------------------------------------
   目录
   -------------------------------------------------------------------------- */
.help__toc {
  position: sticky;
  top: 0;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  padding: var(--lx-space-4);
}

.help__toc-title {
  margin: 0 0 6px;
  font-size: 17px;
}

.help__toc-intro {
  margin: 0 0 16px;
  font-size: 12.5px;
  line-height: 1.6;
}

.help__toc-group {
  margin-bottom: 16px;
}

.help__toc-group-title {
  margin: 0 0 6px 8px;
  color: #7C8CA3;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.2px;
}

.help__toc-item {
  display: block;
  width: 100%;
  padding: 7px 10px;
  border: none;
  border-left: 2px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #B7C4D6;
  font-size: 13.5px;
  text-align: left;
  transition: background 0.15s ease, color 0.15s ease;
}

.help__toc-item:hover {
  background: rgba(52, 192, 141, 0.12);
  color: #FFFFFF;
}

.help__toc-item--active {
  border-left-color: var(--lx-color-primary);
  background: rgba(52, 192, 141, 0.16);
  color: #FFFFFF;
  font-weight: 600;
}

/* --------------------------------------------------------------------------
   正文
   -------------------------------------------------------------------------- */
.help__content {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-4);
  min-width: 0;
}

.help__hero h1 {
  margin: 6px 0 10px;
  font-size: 27px;
}

.help__eyebrow {
  margin: 0;
  color: var(--lx-color-primary-text);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
}

.help__quick {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid var(--lx-color-primary-border);
  border-radius: var(--lx-radius-sm);
  background: var(--lx-color-primary-soft);
}

.help__quick-label {
  display: block;
  margin-bottom: 8px;
  color: var(--lx-color-primary-text);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
}

.help__quick-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
  color: var(--lx-color-text);
  font-size: 14px;
}

.help__topic {
  /* 顶栏是 sticky 的：锚点跳转必须留出它的高度，否则标题会被压在栏下看不见 */
  scroll-margin-top: 84px;
}

.help__topic h2 {
  margin: 4px 0 8px;
  font-size: 21px;
}

.help__topic-group {
  margin: 0;
  color: var(--lx-color-primary-text);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.6px;
}

.help__summary {
  margin: 0 0 6px;
  color: var(--lx-color-text);
  font-size: 15px;
  line-height: 1.7;
}

.help__h3 {
  margin: 20px 0 8px;
  color: var(--lx-color-text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
}

.help__steps {
  margin: 0;
  padding-left: 22px;
  color: var(--lx-color-text);
}

.help__steps li {
  margin-bottom: 7px;
  line-height: 1.75;
}

.help__steps li::marker {
  color: var(--lx-color-primary-text);
  font-weight: 600;
}

.help__notes {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.help__notes li {
  position: relative;
  margin-bottom: 7px;
  padding-left: 18px;
  color: var(--lx-color-text-muted);
  font-size: 13.5px;
  line-height: 1.75;
}

.help__notes li::before {
  content: '!';
  position: absolute;
  left: 0;
  top: 1px;
  display: grid;
  place-items: center;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: rgba(231, 178, 105, 0.2);
  border: 1px solid rgba(231, 178, 105, 0.45);
  color: var(--lx-color-accent);
  font-size: 10px;
  font-weight: 700;
}

.help__topic .lx-button {
  margin-top: 18px;
}

.help__footer {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.help__footer strong {
  color: var(--lx-color-text);
  font-size: 15px;
}

.help__footer p {
  margin: 0;
  font-size: 13.5px;
}

@media (max-width: 1180px) {
  .help {
    grid-template-columns: 1fr;
  }

  .help__toc {
    position: static;
    max-height: none;
  }
}
</style>
