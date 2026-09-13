<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, nextTick } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

/** 关键概念：先让用户理解这套东西怎么运转，再讲点哪里。 */
interface Concept {
  term: string;
  desc: string;
}

/** 流程阶段：一条主线上的第 N 步。 */
interface Stage {
  /** 锚点 id，功能页的「本页怎么用」会深链到这里 */
  id: string;
  /** 阶段序号，形如「第 1 步」 */
  step: string;
  /** 阶段标题 */
  title: string;
  /** 顶部流程图上的短标签（显式给出，避免从标题截字符串） */
  short: string;
  /** 这一阶段要达成什么（为什么做） */
  goal: string;
  /** 用户要做的动作 */
  does: string[];
  /** 系统在这期间做了什么（让用户知道后台并非黑箱） */
  system: string[];
  /** 完成标志：做到什么程度算这一步结束 */
  done: string;
  /** 边界与坑 */
  traps?: string[];
  /** 关联页面 */
  route?: { name: string; label: string };
  /** 相关的参考章节 id（用于「细节见…」） */
  refs?: string[];
}

/** 参考章节：功能级细节，主线之外按需查。 */
interface RefTopic {
  id: string;
  label: string;
  items: string[];
  route?: { name: string; label: string };
}

const concepts: Concept[] = [
  {
    term: '目标',
    desc: '你想达成的事，包含标题和「怎样算成功」。所有行动都挂在某个目标下。'
  },
  {
    term: '里程碑',
    desc: '把目标切成几个阶段，每段有自己的成功标准。可选，但复盘时会按里程碑看进度。'
  },
  {
    term: '行动',
    desc: '重复做的事，例如「每天 21:00 阅读 20 分钟」。要设定重复方式、执行时间与开始日期。'
  },
  {
    term: '行动实例',
    desc: '系统按行动规则生成的具体某一天要做的那件事。打卡打卡的是实例，不是行动本身。'
  },
  {
    term: '打卡',
    desc: '对某个实例反馈结果：完成 / 部分完成 / 跳过。这是系统判断进度与授予成就的唯一依据。'
  },
  {
    term: '周期复盘',
    desc: '系统每周为你激活中的目标生成一份复盘，写下结论才算完成这一周。'
  }
];

const stages: Stage[] = [
  {
    id: 's1-account',
    short: '登录建号',
    step: '第 1 步',
    title: '建立账号，确认你处以哪种模式',
    goal: '先把「我是谁、能不能用核心功能」确定下来。这一步不解决会用多久，只解决能不能开始。',
    does: [
      '打开登录页，填手机号；首次使用还要选出生日期。',
      '点「登录 / 注册」。已注册过的号码只需手机号，出生日期可以留空。'
    ],
    system: [
      '服务端按出生日期判断年龄模式：14 周岁以上才允许注册。',
      '成人账号直接生效；14—17 周岁账号进入「待监护」，需要监护人接受邀请后才解锁创建目标、打卡与灵犀对话。'
    ],
    done: '登录后能看到「今日行动」页，右上角显示「成人模式」或「青少年模式」。',
    traps: [
      '显示「待绑定监护人」时，核心功能是锁住的 —— 先去做第 6 步的监护绑定，不要以为是页面坏了。',
      '当前版本没有短信验证码，手机号即身份；对外正式开放前会补上。'
    ],
    route: { name: 'home', label: '进入今日行动' }
  },
  {
    id: 's2-goal',
    short: '写下目标',
    step: '第 2 步',
    title: '写下一个目标，并把「成功」定义清楚',
    goal: '把模糊的念头变成可判断的句子。「成功标准」写得越具体，后面复盘越不容易陷入自我感觉。',
    does: [
      '进入「我的目标」。',
      '填写标题（例如「每天阅读 20 分钟」）与成功标准（例如「连续 30 天完成阅读」）。',
      '点「创建目标」。'
    ],
    system: [
      '目标以「草稿」状态创建，此时还没有任何每日行动，也不会提醒你。'
    ],
    done: '「我的目标」列表里出现了这条目标，状态是草稿。',
    traps: [
      '草稿状态不会产生行动与提醒 —— 必须完成第 3 步才算真正开始。',
      '成功标准建议写成能被别人判断真假的程度，避免「尽力而为」这类无法验收的表述。'
    ],
    route: { name: 'goals', label: '前往我的目标' }
  },
  {
    id: 's3-plan',
    short: '拆成行动',
    step: '第 3 步',
    title: '把目标拆成行动，然后确认计划',
    goal: '这一步决定了后面每天会看到什么。拆得越贴近真实生活，越容易坚持。',
    does: [
      '在目标列表点进刚创建的目标，进入目标详情。',
      '「里程碑」区可选地添加阶段（标题 + 阶段成功标准）。',
      '「行动」区逐个添加：行动标题、重复方式（一次性 / 每天 / 每周）、执行时间、开始日期，需要时归属到某个里程碑。',
      '核对清单后点「直接确认计划」。若还想改，先点「仅保存草案」，改完再点「激活已保存草案」。'
    ],
    system: [
      '确认会在一次事务内写入计划版本、里程碑与行动，并把目标从草稿激活为执行中。',
      '确认成功后，系统立即按行动规则生成未来 14 天的行动实例（不用等到第二天）。',
      '之后每小时滚动补足后续实例，保证你看到的永远是接下来 14 天。'
    ],
    done: '目标状态变为执行中；回到「今日行动」能看到具体日期与时间的行动。',
    traps: [
      '「直接确认计划」是一次不可回退的提交，提交前请再核对一遍行动清单。',
      '至少要有 1 条行动，按钮才可用。',
      '如果行动的「开始日期」在未来，今日行动里暂时不会出现它，这是正常的。'
    ],
    route: { name: 'goals', label: '前往编排计划' }
  },
  {
    id: 's4-daily',
    short: '每天打卡',
    step: '第 4 步',
    title: '每天打卡，让进度往前走',
    goal: '这是整套系统里你唯一需要「每天做一次」的动作。',
    does: [
      '打开「今日行动」，看未来 7 天的行动实例。',
      '需要留痕时先在「打卡备注」写一句执行感受（可选，会随这次打卡一起记录）。',
      '按实际结果点「完成」「部分完成」或「跳过」。'
    ],
    system: [
      '打卡结果立刻影响目标进度；标记为「完成」时才计入连续打卡。',
      '若触发了成就（目标达成 / 里程碑完成 / 连续打卡），页面顶部弹出「获得新成就」提示。',
      '如果你提前完成了当天行动，原定的行动提醒会被自动取消，不会在你做完之后再催你一次。'
    ],
    done: '当天该做的实例都变成「已记录」，页面底部显示已记录 / 待执行数量。',
    traps: [
      '「部分完成」和「跳过」都不计入连续打卡 —— 连续打卡只统计「完成」。',
      '打卡是按实例记录历史，不是覆盖式状态；跳过也能如实反映当天的真实情况，不会影响后续实例。'
    ],
    route: { name: 'home', label: '前往今日行动' }
  },
  {
    id: 's5-review',
    short: '每周复盘',
    step: '第 5 步',
    title: '每周复盘，把这一周收口',
    goal: '让「做了什么」沉淀成判断，而不是一路往前冲却不知道偏没偏。',
    does: [
      '每周进入「周期复盘」，点「刷新」拿到本周的复盘。',
      '点开某条复盘，写下本周期总结与下一周期要调整的地方，提交。'
    ],
    system: [
      '系统每周一为所有执行中的目标生成一份复盘，记录当周开始时的目标版本与进度作为对照。',
      '同一个目标同一周只会有一份复盘，不会重复生成。'
    ],
    done: '该周复盘显示为已完成。',
    traps: [
      '结论不能留空，写完才能提交。',
      '复盘只针对「执行中」的目标；草稿、暂停、已完成的目标不会生成复盘。'
    ],
    route: { name: 'reviews', label: '前往周期复盘' }
  },
  {
    id: 's6-guardian',
    short: '绑定监护',
    step: '第 6 步（仅青少年）',
    title: '绑定监护人，解锁核心功能',
    goal: '14—17 周岁账号的必经步骤；成人账号可以跳过。',
    does: [
      '青少年：进入「监护」，在「发起监护邀请」点「生成邀请令牌」，把令牌交给监护人。',
      '监护人（须为成人账号）：进入「监护」，在「接受监护邀请」粘贴令牌并点「接受邀请」。'
    ],
    system: [
      '接受后账号从「待监护」变为可用，创建目标、打卡、灵犀对话随之解锁。',
      '监护关系存在未决争议时，接受与撤销都会被冻结，需先由人工处理争议。'
    ],
    done: '账号状态不再是「待绑定监护人」，「今日行动」不再显示门禁提示。',
    traps: [
      '邀请令牌只展示一次，请当场复制给对方。',
      '任何一方都可以在「关系查询」输入关系标识查看状态、撤销关系，或填写理由提交争议。'
    ],
    route: { name: 'guardian', label: '前往监护管理' }
  }
];

const refTopics: RefTopic[] = [
  {
    id: 'r-achievement',
    label: '成就怎么看',
    items: [
      '进入「我的成就」查看已获得的成就，可按类型筛选。',
      '三类成就：目标达成、里程碑完成、连续打卡（默认连续 7 天，以服务端配置为准）。',
      '成就在打卡的同一个事务里授予，打卡成功即已入账，不需要额外操作；同一成就不会重复授予。'
    ],
    route: { name: 'achievements', label: '前往我的成就' }
  },
  {
    id: 'r-companion',
    label: '灵犀对话',
    items: [
      '填会话标题点「创建会话」，再写下你的目标或困惑，点「发送并启动运行」。',
      '运行过程在「执行事件」实时刷新；涉及高风险的动作会先征求同意，你点「确认执行」或「拒绝」。',
      '服务端当前只提供受控 Agent 内核：未配置模型时运行会「失败关闭」而不是编造结果。'
    ],
    route: { name: 'companion', label: '打开灵犀对话' }
  },
  {
    id: 'r-memory',
    label: '记忆管理',
    items: [
      '点「刷新」加载灵犀对你的长期记忆。',
      '某条记忆不对就编辑后保存；不想保留就删除该条。',
      '删除会触发向服务端的删除传播，后续对话不会再引用被删内容。'
    ],
    route: { name: 'memory', label: '前往记忆管理' }
  },
  {
    id: 'r-notification',
    label: '消息与通知偏好',
    items: [
      '「站内消息」查看并标记已读；需要实时接收时点「开始接收」建立实时通道。',
      '「通知偏好」填写场景（例如 ACTION_REMINDER）、时区、免打扰时段与渠道后保存。',
      '换设备或想强制对齐数据时，在「跨端增量变更」点「拉取增量」。',
      '偏好带版本号：两个页面同时改会有一个失败，刷新后重试即可。'
    ],
    route: { name: 'notifications', label: '前往消息与同步' }
  },
  {
    id: 'r-calendar',
    label: '日历同步',
    items: [
      '填写服务端凭据引用（不要粘贴明文令牌）后点「绑定日历」。',
      '撤销时可选择是否同时删除已投影到日历的事件。',
      '真实日历服务商适配尚未接入，当前只完成服务端投影与绑定管理。'
    ],
    route: { name: 'calendar', label: '前往日历同步' }
  },
  {
    id: 'r-partner',
    label: '伙伴与目标授权',
    items: [
      '填入对方用户标识（userId）发出邀请；对方接受后建立伙伴关系。',
      '在「目标授权」选择伙伴关系、目标与过期时间，勾选允许的动作后保存。',
      '「分享链接」需设置晚于当前时刻的过期时间；token 只在创建时返回一次，请立即复制。',
      '伙伴双方都必须是成人账号；授权只覆盖服务端白名单字段，私密对话与目标正文默认不可见。'
    ],
    route: { name: 'partner', label: '前往伙伴与分享' }
  },
  {
    id: 'r-assets',
    label: '内容资产与导入导出',
    items: [
      '在「登记文件」填写文件名与内容摘要，点「登记为待扫描」。',
      '「内容模板」由后台审核发布，客户端只读。',
      '导入导出先在「传输任务」生成预览，确认后才真正写入；导出任务有有效期。',
      '服务端未接入私有对象存储时，登记与导出会返回 CONTENT_STORAGE_UNAVAILABLE 并失败关闭 —— 不会产生假成功。'
    ],
    route: { name: 'assets', label: '前往内容资产' }
  },
  {
    id: 'r-membership',
    label: '会员与权益',
    items: [
      '「可用权益」查看余额与到期时间；「商品」查看价格版本。',
      '订单与订阅可查看历史记录，订阅项可申请取消。',
      '商品目录由运营在后台维护，未上架时列表为空属正常。',
      '真实支付渠道尚未接入，下单与退款目前只完成服务端闭环，不会真实扣款。'
    ],
    route: { name: 'membership', label: '前往会员与权益' }
  },
  {
    id: 'r-support',
    label: '客服工单',
    items: [
      '填写主题（限 120 字符）与详细说明后提交。',
      '写清复现步骤能显著缩短处理时间；工单按 SLA 分级，支付与安全问题优先。'
    ],
    route: { name: 'support', label: '前往客服工单' }
  },
  {
    id: 'r-privacy',
    label: '隐私与账号',
    items: [
      '「隐私权利」选择导出 / 更正 / 删除 / 注销后提交请求；用「刷新」查看进度。',
      '导出完成后可下载加密的限时导出包，过期需重新发起。',
      '注销有冷静期，冷静期内可以撤销。',
      '对年龄判定有异议时，在「年龄申诉」填写证据引用编号后提交。'
    ],
    route: { name: 'privacy', label: '前往隐私与账号' }
  }
];

/** 一周怎么用：把主线落到真实节奏上。 */
const routine: { when: string; what: string }[] = [
  { when: '注册当天（约 10 分钟）', what: '第 1—3 步：登录 → 写下目标 → 拆出 2—3 条日常行动并确认计划。' },
  { when: '每天（约 1 分钟）', what: '第 4 步：打开「今日行动」，把当天该做的实例标记完成 / 部分完成 / 跳过。' },
  { when: '每周一（约 5 分钟）', what: '第 5 步：系统已生成上周复盘，点开写下总结与要调整的地方。' },
  { when: '每月或阶段结束时', what: '回「我的成就」看累计成果；目标达成后可在目标详情里停下或归档。' },
  { when: '卡住的时候', what: '用「灵犀对话」把困惑讲清楚，让它帮你重新拆解；结论不对就去「记忆管理」修正它记住的东西。' }
];

/** 常见疑问 */
const faq: { q: string; a: string }[] = [
  {
    q: '我创建了目标，为什么「今日行动」是空的？',
    a: '两种可能：①目标还是草稿，需要在目标详情点「直接确认计划」；②行动的「开始日期」在将来，实例要到期才会出现。'
  },
  {
    q: '「部分完成」算坚持了吗？',
    a: '算打卡记录，但连续打卡成就只统计标记为「完成」的记录。这是有意的：成就奖励的是完整执行。'
  },
  {
    q: '我提前做完了，还会被提醒吗？',
    a: '不会。提前打卡会取消这条未发送的提醒；已过期的提醒也不会补发，避免打扰。'
  },
  {
    q: '复盘为什么只出现了一部分目标？',
    a: '复盘只针对「执行中」的目标，每周生成一份。草稿、暂停、已完成或已归档的目标不会生成。'
  },
  {
    q: '为什么文件上传 / 导出失败了？',
    a: '服务端还未接入私有对象存储适配器，此时会用 CONTENT_STORAGE_UNAVAILABLE 失败关闭 —— 这是有意为之，避免出现"看起来成功但文件不存在"。'
  },
  {
    q: '为什么灵犀对话运行失败？',
    a: '真实模型与领域工具尚未接入，未配置时运行会失败关闭而不是编造结果。等配置好模型即可正常使用。'
  },
  {
    q: '手机号被人知道了会不会被登录？',
    a: '当前版本确实如此（还没有短信验证码），这是已知的过渡实现，会对外开放前补上。请暂时不要用重要号码注册。'
  }
];

const tocItems = computed(() => [
  { id: 'concepts', label: '先理解这几个词', step: '' },
  ...stages.map((stage) => ({ id: stage.id, label: stage.title, step: stage.step })),
  { id: 'routine', label: '一周怎么用', step: '' },
  { id: 'reference', label: '功能细节参考', step: '' },
  { id: 'faq', label: '常见疑问', step: '' }
]);

/** 功能页 → 主线阶段/参考章节，供顶栏「本页怎么用」深链。 */
const anchorAlias: Record<string, string> = {
  start: 's1-account',
  'guardian-user': 's6-guardian',
  'goal-create': 's2-goal',
  'goal-plan': 's3-plan',
  daily: 's4-daily',
  review: 's5-review',
  achievement: 'r-achievement',
  companion: 'r-companion',
  memory: 'r-memory',
  notification: 'r-notification',
  calendar: 'r-calendar',
  partner: 'r-partner',
  assets: 'r-assets',
  membership: 'r-membership',
  support: 'r-support',
  privacy: 'r-privacy'
};

const activeId = ref<string>('');
const contentEl = ref<HTMLElement | null>(null);
const route = useRoute();

function scrollTo(id: string): void {
  const el = document.getElementById(`topic-${id}`);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    activeId.value = id;
  }
}

function handleScroll(): void {
  let bestId = tocItems.value[0]?.id ?? '';
  let bestDelta = Number.POSITIVE_INFINITY;
  for (const item of tocItems.value) {
    const el = document.getElementById(`topic-${item.id}`);
    if (!el) {
      continue;
    }
    const delta = Math.abs(el.getBoundingClientRect().top - 130);
    if (delta < bestDelta) {
      bestDelta = delta;
      bestId = item.id;
    }
  }
  activeId.value = bestId;
}

onMounted(async () => {
  await nextTick();
  const requested = String(route.query.topic ?? '');
  const resolved = anchorAlias[requested] ?? requested;
  const initial = tocItems.value.some((item) => item.id === resolved) ? resolved : 'concepts';
  activeId.value = initial;
  if (requested) {
    scrollTo(initial);
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
      <p class="lx-muted help__toc-intro">从上往下走一遍，就是完整的用法。</p>
      <nav>
        <button
          v-for="item in tocItems"
          :key="item.id"
          type="button"
          class="help__toc-item"
          :class="{ 'help__toc-item--active': activeId === item.id }"
          @click="scrollTo(item.id)"
        >
          <span v-if="item.step" class="help__toc-step">{{ item.step }}</span>
          <span v-else class="help__toc-step help__toc-step--plain">·</span>
          <span class="help__toc-text">{{ item.label }}</span>
        </button>
      </nav>
    </aside>

    <!-- 正文 -->
    <div ref="contentEl" class="help__content">
      <header class="lx-card help__hero">
        <p class="help__eyebrow">GUIDE</p>
        <h1>灵犀伴行怎么用</h1>
        <p class="lx-muted help__lede">
          这套东西只有一条主线：<strong>写下一个目标 → 拆成每天能做的行动 → 每天打卡 → 每周复盘</strong>。
          下面按这条线一步步走；每步都写清「你要做什么」和「系统同时在做什么」，走完主线再按需查功能细节。
        </p>

        <ol class="help__flow">
          <li v-for="(stage, index) in stages" :key="stage.id" class="help__flow-item">
            <button type="button" class="help__flow-node" @click="scrollTo(stage.id)">
              <span class="help__flow-index">{{ index + 1 }}</span>
              <span class="help__flow-label">{{ stage.short }}</span>
            </button>
            <span v-if="index < stages.length - 1" class="help__flow-arrow">→</span>
          </li>
        </ol>
      </header>

      <!-- 关键概念 -->
      <section id="topic-concepts" class="lx-card help__block">
        <p class="help__stage-step">开始之前</p>
        <h2>先理解这几个词</h2>
        <p class="help__summary">
          这几个词会反复出现，先分清它们，后面所有操作都会顺。
        </p>
        <dl class="help__concepts">
          <div v-for="concept in concepts" :key="concept.term" class="help__concept">
            <dt>{{ concept.term }}</dt>
            <dd>{{ concept.desc }}</dd>
          </div>
        </dl>
      </section>

      <!-- 主线阶段 -->
      <section
        v-for="stage in stages"
        :id="`topic-${stage.id}`"
        :key="stage.id"
        class="lx-card help__block help__block--stage"
      >
        <p class="help__stage-step">{{ stage.step }}</p>
        <h2>{{ stage.title }}</h2>
        <p class="help__summary">{{ stage.goal }}</p>

        <div class="help__two-col">
          <div class="help__col help__col--you">
            <h3 class="help__h3">你要做的</h3>
            <ol class="help__steps">
              <li v-for="(item, index) in stage.does" :key="index">{{ item }}</li>
            </ol>
          </div>
          <div class="help__col help__col--system">
            <h3 class="help__h3">系统在做什么</h3>
            <ul class="help__system">
              <li v-for="(item, index) in stage.system" :key="index">{{ item }}</li>
            </ul>
          </div>
        </div>

        <p class="help__done"><span>做到这里算完成</span>{{ stage.done }}</p>

        <template v-if="stage.traps && stage.traps.length > 0">
          <h3 class="help__h3">容易踩的坑</h3>
          <ul class="help__notes">
            <li v-for="(trap, index) in stage.traps" :key="index">{{ trap }}</li>
          </ul>
        </template>

        <RouterLink v-if="stage.route" class="lx-button lx-button--ghost" :to="{ name: stage.route.name }">
          {{ stage.route.label }}
        </RouterLink>
      </section>

      <!-- 一周节奏 -->
      <section id="topic-routine" class="lx-card help__block">
        <p class="help__stage-step">回到日常</p>
        <h2>一周怎么用</h2>
        <p class="help__summary">主线落到真实节奏上，大概是这样五件事。</p>
        <table class="help__routine">
          <tbody>
            <tr v-for="row in routine" :key="row.when">
              <th>{{ row.when }}</th>
              <td>{{ row.what }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 功能细节参考 -->
      <section id="topic-reference" class="lx-card help__block">
        <p class="help__stage-step">按需查阅</p>
        <h2>功能细节参考</h2>
        <p class="help__summary">
          主线之外的入口，用到再看。每项都标注了当前版本的边界与限制。
        </p>
        <div class="help__refs">
          <article v-for="topic in refTopics" :id="`topic-${topic.id}`" :key="topic.id" class="help__ref">
            <h3>{{ topic.label }}</h3>
            <ul class="help__ref-list">
              <li v-for="(item, index) in topic.items" :key="index">{{ item }}</li>
            </ul>
            <RouterLink v-if="topic.route" class="help__ref-link" :to="{ name: topic.route.name }">
              {{ topic.route.label }} →
            </RouterLink>
          </article>
        </div>
      </section>

      <!-- FAQ -->
      <section id="topic-faq" class="lx-card help__block">
        <p class="help__stage-step">对照排查</p>
        <h2>常见疑问</h2>
        <dl class="help__faq">
          <div v-for="item in faq" :key="item.q" class="help__faq-item">
            <dt>{{ item.q }}</dt>
            <dd>{{ item.a }}</dd>
          </div>
        </dl>
      </section>

      <footer class="lx-card help__footer">
        <strong>还是走不通？</strong>
        <p class="lx-muted">
          在「客服工单」里写清你点到了哪一步、期望的结果、实际看到的结果，我们会按 SLA 跟进。
        </p>
        <RouterLink class="lx-button" :to="{ name: 'support' }">前往客服工单</RouterLink>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.help {
  display: grid;
  grid-template-columns: 262px minmax(0, 1fr);
  gap: var(--lx-space-5);
  align-items: start;
}

/* --------------------------------------------------------------------------
   目录
   -------------------------------------------------------------------------- */
.help__toc {
  position: sticky;
  top: 0;
  max-height: calc(100vh - 160px);
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

.help__toc-item {
  display: flex;
  align-items: center;
  gap: 8px;
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

.help__toc-step {
  flex-shrink: 0;
  color: var(--lx-color-primary-text);
  font-size: 11px;
  font-weight: 700;
}

.help__toc-step--plain {
  color: #7C8CA3;
}

.help__toc-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  margin: 6px 0 12px;
  font-size: 28px;
}

.help__eyebrow {
  margin: 0;
  color: var(--lx-color-primary-text);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
}

.help__lede {
  margin: 0;
  max-width: 860px;
  font-size: 15px;
  line-height: 1.8;
}

.help__lede strong {
  color: var(--lx-color-text);
}

/* 顶部流程条：一眼看完整条主线 */
.help__flow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: 22px 0 0;
  padding: 0;
  list-style: none;
}

.help__flow-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.help__flow-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 13px;
  border: 1px solid var(--lx-color-primary-border);
  border-radius: 999px;
  background: var(--lx-color-primary-soft);
  color: var(--lx-color-text);
  font-size: 13.5px;
  transition: background 0.15s ease, color 0.15s ease;
}

.help__flow-node:hover {
  background: rgba(52, 192, 141, 0.3);
  color: #FFFFFF;
}

.help__flow-index {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--lx-color-primary);
  color: var(--lx-color-text-inverse);
  font-size: 11px;
  font-weight: 700;
}

.help__flow-label {
  white-space: nowrap;
}

.help__flow-arrow {
  color: #7C8CA3;
}

/* 区块 */
.help__block {
  scroll-margin-top: 84px;
}

.help__block h2 {
  margin: 4px 0 8px;
  font-size: 22px;
}

.help__stage-step {
  margin: 0;
  color: var(--lx-color-primary-text);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.6px;
}

.help__summary {
  margin: 0 0 4px;
  max-width: 880px;
  color: var(--lx-color-text);
  font-size: 15px;
  line-height: 1.75;
}

.help__h3 {
  margin: 20px 0 8px;
  color: var(--lx-color-text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
}

/* 你要做的 / 系统在做什么 两栏对照 */
.help__two-col {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  gap: var(--lx-space-4);
  margin-top: 4px;
}

.help__col {
  padding: 16px 18px;
  border: 1px solid var(--lx-color-border);
  border-radius: var(--lx-radius-sm);
  background: rgba(9, 15, 28, 0.4);
}

.help__col--you {
  border-left: 3px solid var(--lx-color-primary);
}

.help__col .help__h3 {
  margin-top: 0;
}

.help__steps {
  margin: 0;
  padding-left: 20px;
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

.help__system {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.help__system li {
  position: relative;
  margin-bottom: 7px;
  padding-left: 16px;
  color: var(--lx-color-text-muted);
  font-size: 13.5px;
  line-height: 1.75;
}

.help__system li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--lx-color-primary);
}

/* 完成标志 */
.help__done {
  display: flex;
  gap: 10px;
  align-items: baseline;
  margin: 16px 0 0;
  padding: 12px 16px;
  border: 1px dashed var(--lx-color-primary-border);
  border-radius: var(--lx-radius-sm);
  color: var(--lx-color-text);
  font-size: 14px;
  line-height: 1.7;
}

.help__done span {
  flex-shrink: 0;
  color: var(--lx-color-primary-text);
  font-size: 12px;
  font-weight: 700;
}

/* 踩坑提示 */
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

/* 概念表 */
.help__concepts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  margin: 16px 0 0;
}

.help__concept {
  padding: 14px 16px;
  border: 1px solid var(--lx-color-border);
  border-radius: var(--lx-radius-sm);
  background: rgba(9, 15, 28, 0.4);
}

.help__concept dt {
  margin-bottom: 6px;
  color: var(--lx-color-primary-text);
  font-size: 14px;
  font-weight: 700;
}

.help__concept dd {
  margin: 0;
  color: var(--lx-color-text-muted);
  font-size: 13.5px;
  line-height: 1.7;
}

/* 一周节奏 */
.help__routine {
  margin: 16px 0 0;
  width: 100%;
}

.help__routine th {
  width: 180px;
  padding: 12px 14px 12px 0;
  color: var(--lx-color-primary-text);
  font-size: 13.5px;
  font-weight: 600;
  text-align: left;
  vertical-align: top;
  white-space: nowrap;
}

.help__routine td {
  padding: 12px 0;
  color: var(--lx-color-text);
  font-size: 14px;
  line-height: 1.7;
}

/* 参考章节 */
.help__refs {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 12px;
}

.help__ref {
  padding: 16px 0;
  border-top: 1px solid rgba(148, 178, 255, 0.1);
  scroll-margin-top: 84px;
}

.help__ref:first-child {
  border-top: none;
  padding-top: 4px;
}

.help__ref h3 {
  margin: 0 0 8px;
  color: var(--lx-color-text);
  font-size: 16px;
  font-weight: 600;
}

.help__ref-list {
  margin: 0;
  padding-left: 18px;
  color: var(--lx-color-text-muted);
  font-size: 13.5px;
}

.help__ref-list li {
  margin-bottom: 6px;
  line-height: 1.7;
}

.help__ref-link {
  display: inline-block;
  margin-top: 8px;
  font-size: 13.5px;
  font-weight: 500;
}

/* FAQ */
.help__faq {
  margin: 16px 0 0;
}

.help__faq-item {
  padding: 14px 0;
  border-top: 1px solid rgba(148, 178, 255, 0.1);
}

.help__faq-item:first-child {
  border-top: none;
  padding-top: 4px;
}

.help__faq-item dt {
  margin-bottom: 6px;
  color: var(--lx-color-text);
  font-size: 15px;
  font-weight: 600;
}

.help__faq-item dd {
  margin: 0;
  color: var(--lx-color-text-muted);
  font-size: 13.5px;
  line-height: 1.75;
}

.help__block .lx-button {
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

@media (max-width: 860px) {
  .help__two-col {
    grid-template-columns: 1fr;
  }

  .help__routine th {
    width: auto;
    padding-right: 12px;
    white-space: normal;
  }
}
</style>
