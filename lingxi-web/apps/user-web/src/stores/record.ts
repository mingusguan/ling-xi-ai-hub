import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type { PageResult, ReviewResult, SupportTicketResult } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

/** 复盘与客服工单列表状态。 */
export const useRecordStore = defineStore('record', () => {
  const reviews = ref<ReviewResult[]>([]);
  /** 复盘总数为服务端 long，按字符串展示，禁止 Number 转换。 */
  const reviewTotal = ref<string>('0');
  const currentReview = ref<ReviewResult | null>(null);
  const reviewGoalFilter = ref<string>('ALL');

  const tickets = ref<SupportTicketResult[]>([]);
  const ticketTotal = ref<string>('0');
  const currentTicket = ref<SupportTicketResult | null>(null);

  const loading = ref(false);
  const errorMessage = ref<string | null>(null);

  const pendingReviews = computed(() =>
    reviews.value.filter((item) => item.status !== 'COMPLETED')
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

  /** 加载本人复盘；goalId 为 'ALL' 时不按目标过滤。 */
  async function loadReviews(page = 1, pageSize = 50): Promise<void> {
    const goalId = reviewGoalFilter.value === 'ALL' ? null : reviewGoalFilter.value;
    const result = await guard<PageResult<ReviewResult>>(() =>
      api.goals.listReviews(goalId, page, pageSize)
    );
    if (result) {
      reviews.value = result.items;
      reviewTotal.value = result.total;
    }
  }

  /** 加载复盘详情。 */
  async function loadReview(reviewId: string): Promise<void> {
    const result = await guard(() => api.goals.getReview(reviewId));
    if (result) {
      currentReview.value = result;
    }
  }

  /** 提交复盘结论。 */
  async function completeReview(reviewId: string, conclusion: string): Promise<ReviewResult | null> {
    const updated = await guard(() =>
      api.goals.completeReview(
        reviewId,
        { conclusionJson: JSON.stringify({ summary: conclusion }) },
        api.http.newIdempotencyKey()
      )
    );
    if (updated) {
      reviews.value = reviews.value.map((item) =>
        item.reviewId === updated.reviewId ? updated : item
      );
      currentReview.value = updated;
    }
    return updated;
  }

  /** 加载本人工单列表。 */
  async function loadTickets(page = 1, pageSize = 20): Promise<void> {
    const result = await guard<PageResult<SupportTicketResult>>(() =>
      api.support.listTickets(page, pageSize)
    );
    if (result) {
      tickets.value = result.items;
      ticketTotal.value = result.total;
    }
  }

  /** 加载工单详情。 */
  async function loadTicket(ticketId: string): Promise<void> {
    const result = await guard(() => api.support.getTicket(ticketId));
    if (result) {
      currentTicket.value = result;
    }
  }

  return {
    reviews,
    reviewTotal,
    currentReview,
    reviewGoalFilter,
    pendingReviews,
    tickets,
    ticketTotal,
    currentTicket,
    loading,
    errorMessage,
    loadReviews,
    loadReview,
    completeReview,
    loadTickets,
    loadTicket
  };
});
