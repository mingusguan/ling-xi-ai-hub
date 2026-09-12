import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type {
  EntitlementResult,
  OrderResult,
  ProductResult,
  SubscriptionResult
} from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

/** 会员商品、订单、订阅与权益状态。 */
export const useMembershipStore = defineStore('membership', () => {
  const products = ref<ProductResult[]>([]);
  const orders = ref<OrderResult[]>([]);
  /** 订单总数为服务端 long，按字符串展示，禁止 Number 转换。 */
  const orderTotal = ref<string>('0');
  const subscriptions = ref<SubscriptionResult[]>([]);
  const subscriptionTotal = ref<string>('0');
  const entitlements = ref<EntitlementResult[]>([]);
  const loading = ref(false);
  const errorMessage = ref<string | null>(null);

  /** 当前仍在生效的订阅（含到期自动取消）。 */
  const activeSubscriptions = computed(() =>
    subscriptions.value.filter(
      (item) => item.status === 'ACTIVE' || item.status === 'CANCEL_AT_PERIOD_END'
    )
  );

  /** 余额大于 0 且未过期的权益。 */
  const usableEntitlements = computed(() =>
    entitlements.value.filter(
      (item) => BigIntSafePositive(item.balance) && !expired(item.expiresAt)
    )
  );

  async function guard<T>(action: () => Promise<T>): Promise<T | null> {
    const session = useSessionStore();
    loading.value = true;
    errorMessage.value = null;
    try {
      await session.ensureFreshToken();
      return await action();
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '请求失败';
      return null;
    } finally {
      loading.value = false;
    }
  }

  /** 加载当前场景可购买商品。 */
  async function loadProducts(scene = 'DEFAULT'): Promise<void> {
    const result = await guard(() => api.commerce.listProducts(scene));
    if (result) {
      products.value = result;
    }
  }

  /** 加载本人订单。 */
  async function loadOrders(page = 1, pageSize = 20): Promise<void> {
    const result = await guard(() => api.commerce.listOrders(page, pageSize));
    if (result) {
      orders.value = result.items;
      orderTotal.value = result.total;
    }
  }

  /** 加载本人订阅。 */
  async function loadSubscriptions(page = 1, pageSize = 20): Promise<void> {
    const result = await guard(() => api.commerce.listSubscriptions(page, pageSize));
    if (result) {
      subscriptions.value = result.items;
      subscriptionTotal.value = result.total;
    }
  }

  /** 加载本人权益总览。 */
  async function loadEntitlements(): Promise<void> {
    const result = await guard(() => api.commerce.listEntitlements());
    if (result) {
      entitlements.value = result;
    }
  }

  /** 创建订单；不含支付渠道适配器时服务端会在支付阶段失败关闭。 */
  async function createOrder(
    product: ProductResult,
    channel = 'MANUAL'
  ): Promise<OrderResult | null> {
    const created = await guard(() =>
      api.commerce.createOrder(
        {
          productId: product.productId,
          priceVersion: product.priceVersion,
          channel,
          guardianApprovalReference: null
        },
        api.http.newIdempotencyKey()
      )
    );
    if (created) {
      orders.value = [created, ...orders.value];
    }
    return created;
  }

  /** 取消订阅，需要近期认证。 */
  async function cancelSubscription(
    subscription: SubscriptionResult,
    cancelMode: 'IMMEDIATE' | 'AT_PERIOD_END'
  ): Promise<SubscriptionResult | null> {
    const updated = await guard(() =>
      api.commerce.cancelSubscription(
        subscription.subscriptionId,
        cancelMode,
        subscription.version,
        api.http.newIdempotencyKey()
      )
    );
    if (updated) {
      subscriptions.value = subscriptions.value.map((item) =>
        item.subscriptionId === updated.subscriptionId ? updated : item
      );
    }
    return updated;
  }

  return {
    products,
    orders,
    orderTotal,
    subscriptions,
    subscriptionTotal,
    entitlements,
    activeSubscriptions,
    usableEntitlements,
    loading,
    errorMessage,
    loadProducts,
    loadOrders,
    loadSubscriptions,
    loadEntitlements,
    createOrder,
    cancelSubscription
  };
});

/** 权益余额为服务端 long 字符串，避免 Number 精度问题下判断是否为正。 */
function BigIntSafePositive(value: string): boolean {
  try {
    return BigInt(value) > 0n;
  } catch {
    return false;
  }
}

function expired(expiresAt: string | null): boolean {
  return expiresAt !== null && new Date(expiresAt).getTime() <= Date.now();
}
