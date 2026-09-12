import type { ApiClient, LongId, PageResult } from '../http';
import type {
  EntitlementResult,
  OrderResult,
  ProductResult,
  SubscriptionResult
} from '../types';

/** 商品列表与订单/订阅/权益接口客户端。 */
export class CommerceApi {
  constructor(private readonly client: ApiClient) {}

  /** 按场景查询当前适用商品与价格版本；服务端按年龄策略过滤。 */
  listProducts(scene = 'DEFAULT'): Promise<ProductResult[]> {
    return this.client.send<ProductResult[]>('/api/v1/products', { query: { scene } });
  }

  /** 分页查询本人订单，按创建时间倒序。 */
  listOrders(page = 1, pageSize = 20): Promise<PageResult<OrderResult>> {
    return this.client.send<PageResult<OrderResult>>('/api/v1/orders', {
      query: { page, pageSize }
    });
  }

  /** 创建订单；businessOrderKey 由调用方生成并在重试时复用。 */
  createOrder(
    body: {
      productId: LongId;
      priceVersion: number;
      channel: string;
      guardianApprovalReference: string | null;
    },
    idempotencyKey: string
  ): Promise<OrderResult> {
    return this.client.send<OrderResult>('/api/v1/orders', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 分页查询本人订阅，含已取消与已过期记录。 */
  listSubscriptions(page = 1, pageSize = 20): Promise<PageResult<SubscriptionResult>> {
    return this.client.send<PageResult<SubscriptionResult>>('/api/v1/subscriptions', {
      query: { page, pageSize }
    });
  }

  /** 取消订阅；需要近期认证，expectedVersion 用于乐观并发。 */
  cancelSubscription(
    subscriptionId: LongId,
    cancelMode: 'IMMEDIATE' | 'AT_PERIOD_END',
    expectedVersion: LongId,
    idempotencyKey: string
  ): Promise<SubscriptionResult> {
    return this.client.send<SubscriptionResult>(
      `/api/v1/subscriptions/${subscriptionId}/cancel`,
      {
        method: 'POST',
        body: { cancelMode, expectedVersion },
        idempotencyKey
      }
    );
  }

  /** 查询本人全部权益余额，供会员页一次展示权益总览。 */
  listEntitlements(): Promise<EntitlementResult[]> {
    return this.client.send<EntitlementResult[]>('/api/v1/entitlements');
  }

  /** 查询单个资源权益；未开通时 balance 为 0。 */
  getEntitlement(resource: string): Promise<EntitlementResult> {
    return this.client.send<EntitlementResult>(`/api/v1/entitlements/${resource}`);
  }
}
