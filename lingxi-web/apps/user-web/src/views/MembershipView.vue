<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useMembershipStore } from '@/stores/membership';

const membership = useMembershipStore();
const notice = ref<string | null>(null);
const cancellingId = ref<string | null>(null);

const billingLabels: Record<string, string> = {
  ONE_TIME: '一次性',
  MONTHLY: '按月订阅',
  QUARTERLY: '按季订阅',
  YEARLY: '按年订阅'
};

const orderStatusLabels: Record<string, string> = {
  CREATED: '待支付',
  PAYING: '支付确认中',
  PAID: '已支付',
  CLOSED: '已关闭',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  FAILED: '支付失败'
};

const subscriptionStatusLabels: Record<string, string> = {
  ACTIVE: '生效中',
  CANCEL_AT_PERIOD_END: '到期后取消',
  PAUSED: '已暂停',
  SUSPENDED: '已挂起',
  CANCELLED: '已取消',
  EXPIRED: '已过期'
};

/** 服务端金额为最小币种的 long，展示时只做字符串拼接，避免精度问题。 */
function amount(value: string, currency: string): string {
  return `${currency === 'CNY' ? '¥' : `${currency} `}${value} 分`;
}

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN') : '—';
}

const hasProducts = computed(() => membership.products.length > 0);

onMounted(async () => {
  await Promise.all([
    membership.loadProducts(),
    membership.loadOrders(),
    membership.loadSubscriptions(),
    membership.loadEntitlements()
  ]);
});

async function buy(productId: string): Promise<void> {
  const product = membership.products.find((item) => item.productId === productId);
  if (!product) {
    return;
  }
  notice.value = null;
  const created = await membership.createOrder(product);
  notice.value = created
    ? `订单 ${created.orderNo} 已创建，状态 ${orderStatusLabels[created.status] ?? created.status}；接入支付渠道后在此发起支付`
    : membership.errorMessage;
}

async function cancel(subscriptionId: string, mode: 'IMMEDIATE' | 'AT_PERIOD_END'): Promise<void> {
  const subscription = membership.subscriptions.find(
    (item) => item.subscriptionId === subscriptionId
  );
  if (!subscription) {
    return;
  }
  cancellingId.value = subscriptionId;
  notice.value = null;
  try {
    const updated = await membership.cancelSubscription(subscription, mode);
    notice.value = updated
      ? `订阅状态已更新为 ${subscriptionStatusLabels[updated.status] ?? updated.status}`
      : membership.errorMessage;
  } finally {
    cancellingId.value = null;
  }
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row membership__head">
      <div>
        <h2>会员与权益</h2>
        <p class="lx-muted">
          商品由服务端按账号年龄策略过滤（青少年仅可购买一次性商品）；订阅取消需要近期认证，
          未接入支付渠道时订单停留在待支付状态。
        </p>
      </div>
      <span class="lx-tag">{{ membership.activeSubscriptions.length }} 个生效订阅</span>
    </header>

    <p v-if="membership.errorMessage" class="lx-error">{{ membership.errorMessage }}</p>
    <p v-if="notice" class="lx-muted">{{ notice }}</p>

    <h3 class="membership__section">可用权益</h3>
    <div v-if="membership.entitlements.length === 0" class="lx-empty">暂无权益记录。</div>
    <div v-else class="lx-grid">
      <article
        v-for="item in membership.entitlements"
        :key="item.resourceKey"
        class="lx-card membership__entitlement"
      >
        <strong>{{ item.resourceKey }}</strong>
        <span class="lx-tag">余额 {{ item.balance }}</span>
        <p class="lx-muted">到期：{{ formatTime(item.expiresAt) }}</p>
      </article>
    </div>

    <h3 class="membership__section">商品</h3>
    <div v-if="!hasProducts" class="lx-empty">当前场景没有可用商品。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>商品</th>
          <th>场景</th>
          <th>周期</th>
          <th>价格</th>
          <th>年龄策略</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="product in membership.products" :key="product.productId">
          <td>{{ product.name }}</td>
          <td>{{ product.scene }}</td>
          <td>{{ billingLabels[product.billingPeriod] ?? product.billingPeriod }}</td>
          <td>{{ amount(product.amountMinor, product.currency) }}</td>
          <td>{{ product.agePolicy }}</td>
          <td>
            <button class="lx-button lx-button--ghost" @click="buy(product.productId)">
              创建订单
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <h3 class="membership__section">
      我的订阅 <span class="lx-tag">共 {{ membership.subscriptionTotal }} 条</span>
    </h3>
    <div v-if="membership.subscriptions.length === 0" class="lx-empty">暂无订阅记录。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>订阅号</th>
          <th>商品</th>
          <th>状态</th>
          <th>当前周期结束</th>
          <th>取消方式</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in membership.subscriptions" :key="item.subscriptionId">
          <td>{{ item.subscriptionId }}</td>
          <td>{{ item.productId }}</td>
          <td>
            <span
              class="lx-tag"
              :class="{ 'lx-tag--warn': item.status !== 'ACTIVE' }"
            >
              {{ subscriptionStatusLabels[item.status] ?? item.status }}
            </span>
          </td>
          <td>{{ formatTime(item.periodEnd) }}</td>
          <td>{{ item.cancelMode ?? '—' }}</td>
          <td class="lx-row">
            <button
              v-if="item.status === 'ACTIVE'"
              class="lx-button lx-button--ghost"
              :disabled="cancellingId === item.subscriptionId"
              @click="cancel(item.subscriptionId, 'AT_PERIOD_END')"
            >
              到期取消
            </button>
            <button
              v-if="item.status === 'ACTIVE' || item.status === 'CANCEL_AT_PERIOD_END'"
              class="lx-button lx-button--ghost"
              :disabled="cancellingId === item.subscriptionId"
              @click="cancel(item.subscriptionId, 'IMMEDIATE')"
            >
              立即取消
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <h3 class="membership__section">
      我的订单 <span class="lx-tag">共 {{ membership.orderTotal }} 条</span>
    </h3>
    <div v-if="membership.orders.length === 0" class="lx-empty">暂无订单记录。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>订单号</th>
          <th>金额</th>
          <th>状态</th>
          <th>已退款</th>
          <th>支付参考号</th>
          <th>创建时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in membership.orders" :key="order.orderId">
          <td>{{ order.orderNo }}</td>
          <td>{{ amount(order.amountMinor, order.currency) }}</td>
          <td>
            <span class="lx-tag" :class="{ 'lx-tag--warn': order.status === 'CREATED' }">
              {{ orderStatusLabels[order.status] ?? order.status }}
            </span>
          </td>
          <td>{{ order.refundedMinor }}</td>
          <td>{{ order.paymentReference ?? '—' }}</td>
          <td>{{ formatTime(order.createdAt) }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.membership__head {
  justify-content: space-between;
}

.membership__section {
  margin: var(--lx-space-5) 0 var(--lx-space-3);
}

.membership__entitlement {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-2);
}
</style>
