import {
  AchievementResult,
  CalendarBindingResult,
  EntitlementResult,
  FileResult,
  GoalResult,
  MemoryResult,
  NotificationResult,
  OccurrenceResult,
  OrderResult,
  PartnerGrantResult,
  PartnerRelationResult,
  ReviewResult,
  ShareLinkResult,
  SubscriptionResult,
  TemplateVersionResult
} from '../model/Types';
import { SessionStore } from '../store/SessionStore';

/** 首屏与登录页：未登录时展示手机号登录，登录后进入 Tab 主界面。 */
@Entry
@Component
struct Index {
  @State authenticated: boolean = false;
  @State phoneNumber: string = '';
  @State birthDate: string = '';
  @State assertion: string = '';
  @State message: string = '';
  @State tabIndex: number = 0;
  @State goals: GoalResult[] = [];
  @State occurrences: OccurrenceResult[] = [];
  @State achievements: AchievementResult[] = [];
  @State notifications: NotificationResult[] = [];
  @State reviews: ReviewResult[] = [];
  @State memories: MemoryResult[] = [];
  @State orders: OrderResult[] = [];
  @State subscriptions: SubscriptionResult[] = [];
  @State entitlements: EntitlementResult[] = [];
  @State files: FileResult[] = [];
  @State templates: TemplateVersionResult[] = [];
  @State relations: PartnerRelationResult[] = [];
  @State grants: PartnerGrantResult[] = [];
  @State shares: ShareLinkResult[] = [];
  @State calendars: CalendarBindingResult[] = [];
  @State busy: boolean = false;

  aboutToAppear(): void {
    this.authenticated = SessionStore.isAuthenticated();
    if (this.authenticated) {
      this.refreshAll();
    }
  }

  /** 手机号登录；首次登录需要出生日期，服务端据此完成 14+ 准入判定。 */
  async loginByPhone(): Promise<void> {
    const phone = this.phoneNumber.replace(/[\s-]/g, '');
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      this.message = '请输入有效的中国大陆手机号';
      return;
    }
    this.busy = true;
    try {
      await SessionStore.loginWithPhone(phone, this.birthDate);
      this.authenticated = true;
      this.message = '';
      await this.refreshAll();
    } catch (error) {
      this.message = `登录失败：${JSON.stringify(error)}`;
    } finally {
      this.busy = false;
    }
  }

  async loginByAssertion(register: boolean): Promise<void> {
    if (this.assertion.length === 0) {
      this.message = '请先提供身份断言（由受信身份桥接层签发，本地联调可手工粘贴）';
      return;
    }
    this.busy = true;
    try {
      if (register) {
        await SessionStore.registerAndLogin(this.assertion, 'Asia/Shanghai');
      } else {
        await SessionStore.login(this.assertion);
      }
      this.authenticated = true;
      this.message = '';
      await this.refreshAll();
    } catch (error) {
      this.message = `登录失败：${JSON.stringify(error)}`;
    } finally {
      this.busy = false;
    }
  }

  async refreshAll(): Promise<void> {
    try {
      this.goals = await SessionStore.api.listGoals();
      const today = new Date().toISOString().slice(0, 10);
      const weekEnd = new Date(Date.now() + 6 * 24 * 3600 * 1000).toISOString().slice(0, 10);
      this.occurrences = await SessionStore.api.listOccurrences(today, weekEnd);
      const page = await SessionStore.api.listAchievements(null, 1, 50);
      this.achievements = page.items;
      this.notifications = await SessionStore.api.listNotifications(0, 50);
      const reviewPage = await SessionStore.api.listReviews(null, 1, 50);
      this.reviews = reviewPage.items;
      this.memories = await SessionStore.api.listMemories();
      const orderPage = await SessionStore.api.listOrders(1, 50);
      this.orders = orderPage.items;
      const subscriptionPage = await SessionStore.api.listSubscriptions(1, 50);
      this.subscriptions = subscriptionPage.items;
      this.entitlements = await SessionStore.api.listEntitlements();
      const filePage = await SessionStore.api.listMyFiles(1, 50);
      this.files = filePage.items;
      this.templates = await SessionStore.api.listTemplates();
      const relationPage = await SessionStore.api.listPartnerRelations(1, 50);
      this.relations = relationPage.items;
      this.grants = await SessionStore.api.listPartnerGrants();
      const sharePage = await SessionStore.api.listShares(1, 50);
      this.shares = sharePage.items;
      this.calendars = await SessionStore.api.listCalendarBindings();
    } catch (error) {
      this.message = `数据加载失败：${JSON.stringify(error)}`;
    }
  }

  /**
   * 删除长期记忆。
   *
   * 服务端按 `expectedVersion` 做乐观并发校验，版本必须来自最近一次查询结果。
   */
  async removeMemory(memoryId: string, expectedVersion: string): Promise<void> {
    try {
      await SessionStore.api.deleteMemory(memoryId, expectedVersion);
      this.message = '删除请求已受理，服务端会异步传播';
      await this.refreshAll();
    } catch (error) {
      this.message = `删除失败：${JSON.stringify(error)}`;
    }
  }

  /** 接受伙伴邀请。 */
  async acceptRelation(relationId: string, expectedVersion: string): Promise<void> {
    try {
      await SessionStore.api.acceptPartnerRelation(relationId, expectedVersion);
      this.message = '邀请已接受';
      await this.refreshAll();
    } catch (error) {
      this.message = `接受邀请失败：${JSON.stringify(error)}`;
    }
  }

  /** 撤销分享链接。 */
  async revokeShare(shareId: string, expectedVersion: string): Promise<void> {
    try {
      await SessionStore.api.revokeShare(shareId, expectedVersion);
      this.message = '分享链接已撤销';
      await this.refreshAll();
    } catch (error) {
      this.message = `撤销分享失败：${JSON.stringify(error)}`;
    }
  }

  async checkIn(occurrenceId: number, result: 'COMPLETED' | 'PARTIAL' | 'SKIPPED'): Promise<void> {
    try {
      const checkIn = await SessionStore.api.checkIn(occurrenceId, result, null, false);
      this.message =
        checkIn.newAchievements.length > 0
          ? `获得新成就：${checkIn.newAchievements.map((item) => item.title).join('、')}`
          : '打卡已记录';
      await this.refreshAll();
    } catch (error) {
      this.message = `打卡失败：${JSON.stringify(error)}`;
    }
  }

  async offlineCheckIn(occurrenceId: number): Promise<void> {
    // 离线队列与 PC Web 使用同一命令契约：CHECK_IN + clientCommandId 幂等重放。
    const payload = JSON.stringify({
      occurrenceId: occurrenceId,
      result: 'COMPLETED',
      note: null,
      evidenceReference: null,
      correction: false
    });
    await SessionStore.api.http.send<object>('/api/v1/sync/commands', {
      body: {
        clientCommandId: SessionStore.api.http.newIdempotencyKey(),
        commandType: 'CHECK_IN',
        payloadJson: payload,
        baseVersion: 0
      }
    });
    this.message = '离线命令已提交，服务端按键幂等重放';
    await this.refreshAll();
  }

  build() {
    Column() {
      if (!this.authenticated) {
        this.LoginPanel()
      } else {
        this.MainShell()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F6F4EF')
  }

  @Builder
  LoginPanel() {
    Column({ space: 12 }) {
      Text('灵犀伴行').fontSize(24).fontWeight(FontWeight.Bold)
      Text('使用手机号登录；首次登录会自动创建账号，需要填写出生日期（服务端强制 14 周岁以上准入）。')
        .fontSize(13)
        .fontColor('#6D675D')
      TextInput({ placeholder: '手机号，例如 13800000000' })
        .type(InputType.PhoneNumber)
        .onChange((value: string) => {
          this.phoneNumber = value;
        })
      Text('出生日期（YYYY-MM-DD，已注册可留空）').fontSize(12).fontColor('#6D675D')
      TextInput({ placeholder: '1990-01-01' })
        .onChange((value: string) => {
          this.birthDate = value;
        })
      Button('登录 / 注册')
        .width('100%')
        .enabled(!this.busy)
        .onClick(() => {
          this.loginByPhone();
        })
      Text('身份断言登录（身份桥接层联调）：').fontSize(12).fontColor('#6D675D')
      TextArea({ placeholder: 'base64url(payload).base64url(hmacSha256(payload))' })
        .height(80)
        .onChange((value: string) => {
          this.assertion = value;
        })
      Row({ space: 8 }) {
        Button('断言登录')
          .backgroundColor('#E3EFE9')
          .fontColor('#2F6F5E')
          .enabled(!this.busy)
          .onClick(() => {
            this.loginByAssertion(false);
          })
        Button('断言注册并登录')
          .backgroundColor('#E3EFE9')
          .fontColor('#2F6F5E')
          .enabled(!this.busy)
          .onClick(() => {
            this.loginByAssertion(true);
          })
      }
      if (this.message.length > 0) {
        Text(this.message).fontColor('#B4453B').fontSize(13)
      }
    }
    .padding(24)
    .width('100%')
  }

  @Builder
  MainShell() {
    Tabs({ barPosition: BarPosition.End, index: this.tabIndex }) {
      TabContent() {
        this.TodayTab()
      }.tabBar('今日')

      TabContent() {
        this.GoalsTab()
      }.tabBar('目标')

      TabContent() {
        this.ReviewsTab()
      }.tabBar('复盘')

      TabContent() {
        this.AchievementsTab()
      }.tabBar('成就')

      TabContent() {
        this.MessagesTab()
      }.tabBar('消息')

      TabContent() {
        this.MemoryTab()
      }.tabBar('记忆')

      TabContent() {
        this.PartnerTab()
      }.tabBar('伙伴')

      TabContent() {
        this.MembershipTab()
      }.tabBar('会员')

      TabContent() {
        this.AssetsTab()
      }.tabBar('资产')

      TabContent() {
        this.CalendarTab()
      }.tabBar('日历')
    }
    .width('100%')
    .height('100%')
    .barMode(BarMode.Scrollable)
    .onChange((index: number) => {
      this.tabIndex = index;
    })
  }

  @Builder
  TodayTab() {
    Column({ space: 12 }) {
      Text(`今日行动（${this.occurrences.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      if (this.message.length > 0) {
        Text(this.message).fontColor('#2F6F5E').fontSize(13)
      }
      List({ space: 8 }) {
        ForEach(this.occurrences, (item: OccurrenceResult) => {
          ListItem() {
            Row() {
              Column({ space: 4 }) {
                Text(item.actionTitle).fontSize(15)
                Text(`${item.localDate} · ${item.timezone} · ${item.status}`)
                  .fontSize(12)
                  .fontColor('#6D675D')
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              if (item.status === 'SCHEDULED') {
                Button('完成')
                  .fontSize(12)
                  .onClick(() => {
                    this.checkIn(item.occurrenceId, 'COMPLETED');
                  })
              }
            }
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
      Button('刷新')
        .backgroundColor('#E3EFE9')
        .fontColor('#2F6F5E')
        .onClick(() => {
          this.refreshAll();
        })
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  GoalsTab() {
    Column({ space: 12 }) {
      Text(`我的目标（${this.goals.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      List({ space: 8 }) {
        ForEach(this.goals, (goal: GoalResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(goal.title).fontSize(15)
              Text(`${goal.status} · 进度 ${goal.progress}% · 版本 ${goal.version}`)
                .fontSize(12)
                .fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  AchievementsTab() {
    Column({ space: 12 }) {
      Text(`我的成就（${this.achievements.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      Text('确定性成就：目标达成、里程碑完成、连续打卡达阈值，授予后不可撤销。')
        .fontSize(12)
        .fontColor('#6D675D')
      List({ space: 8 }) {
        ForEach(this.achievements, (item: AchievementResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(item.title).fontSize(15)
              Text(`${item.type} · ${item.description ?? ''}`).fontSize(12).fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  MessagesTab() {
    Column({ space: 12 }) {
      Text(`站内消息（${this.notifications.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      List({ space: 8 }) {
        ForEach(this.notifications, (item: NotificationResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(item.summary).fontSize(15)
              Text(`${item.type} · ${item.createdAt}`).fontSize(12).fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
            .onClick(() => {
              SessionStore.api.markNotificationRead(item.id).then(() => {
                this.refreshAll();
              });
            })
          }
        })
      }
      .layoutWeight(1)
      Button('退出登录')
        .backgroundColor('#E3EFE9')
        .fontColor('#2F6F5E')
        .onClick(() => {
          SessionStore.logout().then(() => {
            this.authenticated = false;
          });
        })
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  ReviewsTab() {
    Column({ space: 12 }) {
      Text(`周期复盘（${this.reviews.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      Text('复盘由服务端按周期生成；完成后写入结论，结论作为历史记录保留。')
        .fontSize(12)
        .fontColor('#6D675D')
      List({ space: 8 }) {
        ForEach(this.reviews, (item: ReviewResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`${item.periodKey} · ${item.status}`).fontSize(15)
              Text(`目标 ${item.goalId}`).fontSize(12).fontColor('#6D675D')
              Text(`完成时间 ${item.completedAt ?? '—'}`).fontSize(12).fontColor('#6D675D')
              if (item.inputSnapshotJson !== null) {
                Text(`输入快照 ${item.inputSnapshotJson}`)
                  .fontSize(12)
                  .fontColor('#6D675D')
                  .maxLines(3)
              }
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
      Button('刷新')
        .backgroundColor('#E3EFE9')
        .fontColor('#2F6F5E')
        .onClick(() => {
          this.refreshAll();
        })
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  MemoryTab() {
    Column({ space: 12 }) {
      Text(`记忆管理（${this.memories.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      Text('长期记忆可查看、更正或删除；删除会传播到相关会话与派生数据。')
        .fontSize(12)
        .fontColor('#6D675D')
      List({ space: 8 }) {
        ForEach(this.memories, (item: MemoryResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(item.contentText).fontSize(15)
              Text(`${item.purpose} · 敏感度 ${item.sensitivity} · ${item.status}`)
                .fontSize(12)
                .fontColor('#6D675D')
              Row({ space: 8 }) {
                Button('删除')
                  .fontSize(12)
                  .backgroundColor('#F2DEDA')
                  .fontColor('#B4453B')
                  .enabled(item.status === 'ACTIVE')
                  .onClick(() => {
                    this.removeMemory(item.id, item.version);
                  })
              }
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  PartnerTab() {
    Column({ space: 12 }) {
      Text(`伙伴与分享（${this.relations.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      Text('伙伴双方必须为成人账号；分享必须设置过期时间，token 只在创建时返回一次。')
        .fontSize(12)
        .fontColor('#6D675D')
      List({ space: 8 }) {
        ForEach(this.relations, (item: PartnerRelationResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`关系 ${item.relationId} · ${item.status}`).fontSize(15)
              Text(`邀请方 ${item.inviterUserId} → 被邀请方 ${item.inviteeUserId}`)
                .fontSize(12)
                .fontColor('#6D675D')
              if (item.status === 'INVITED') {
                Button('接受邀请')
                  .fontSize(12)
                  .onClick(() => {
                    this.acceptRelation(item.relationId, item.version);
                  })
              }
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
      Text(`已发出授权（${this.grants.length}）`).fontSize(15).fontWeight(FontWeight.Medium)
      List({ space: 8 }) {
        ForEach(this.grants, (item: PartnerGrantResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`授权 ${item.grantId} · 目标 ${item.goalId} · ${item.status}`).fontSize(14)
              Text(`权限 ${item.permissions.join('、')}`).fontSize(12).fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
      Text(`分享链接（${this.shares.length}）`).fontSize(15).fontWeight(FontWeight.Medium)
      List({ space: 8 }) {
        ForEach(this.shares, (item: ShareLinkResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`${item.resourceType}/${item.resourceId} · ${item.status}`).fontSize(14)
              Text(`字段 ${item.fields.join('、')} · 访问 ${item.visitCount}/${item.visitLimit ?? '不限'}`)
                .fontSize(12)
                .fontColor('#6D675D')
              Text(`过期 ${item.expiresAt ?? '—'}`).fontSize(12).fontColor('#6D675D')
              if (item.status === 'ACTIVE') {
                Button('撤销分享')
                  .fontSize(12)
                  .backgroundColor('#F2DEDA')
                  .fontColor('#B4453B')
                  .onClick(() => {
                    this.revokeShare(item.shareId, item.version);
                  })
              }
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  MembershipTab() {
    Column({ space: 12 }) {
      Text(`会员与权益（${this.entitlements.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      Text('商品由服务端按年龄策略过滤；未接入支付渠道时订单停留在待支付状态。')
        .fontSize(12)
        .fontColor('#6D675D')
      Text(`可用权益`).fontSize(15).fontWeight(FontWeight.Medium)
      List({ space: 8 }) {
        ForEach(this.entitlements, (item: EntitlementResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`${item.resourceKey} · 余额 ${item.balance}`).fontSize(14)
              Text(`到期 ${item.expiresAt ?? '—'}`).fontSize(12).fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
      Text(`我的订阅（${this.subscriptions.length}）`).fontSize(15).fontWeight(FontWeight.Medium)
      List({ space: 8 }) {
        ForEach(this.subscriptions, (item: SubscriptionResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`订阅 ${item.subscriptionId} · ${item.status}`).fontSize(14)
              Text(`周期结束 ${item.periodEnd ?? '—'} · 取消方式 ${item.cancelMode ?? '—'}`)
                .fontSize(12)
                .fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
      Text(`我的订单（${this.orders.length}）`).fontSize(15).fontWeight(FontWeight.Medium)
      List({ space: 8 }) {
        ForEach(this.orders, (item: OrderResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`${item.orderNo} · ${item.status}`).fontSize(14)
              Text(`金额 ${item.amountMinor} ${item.currency} · 已退款 ${item.refundedMinor}`)
                .fontSize(12)
                .fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  AssetsTab() {
    Column({ space: 12 }) {
      Text(`内容资产（${this.files.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      Text('文件需登记并扫描就绪后才可被引用；未接入私有对象存储时服务端失败关闭。')
        .fontSize(12)
        .fontColor('#6D675D')
      List({ space: 8 }) {
        ForEach(this.files, (item: FileResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(item.originalName).fontSize(15)
              Text(`${item.purpose} · ${item.status} · ${item.sizeBytes} 字节`)
                .fontSize(12)
                .fontColor('#6D675D')
              Text(`敏感级别 ${item.sensitivity}`).fontSize(12).fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
      Text(`内容模板（只读，${this.templates.length}）`).fontSize(15).fontWeight(FontWeight.Medium)
      List({ space: 8 }) {
        ForEach(this.templates, (item: TemplateVersionResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`模板 ${item.templateId} · v${item.versionNo}`).fontSize(14)
              Text(`${item.ageScope} · ${item.status} · 发布 ${item.publishedAt ?? '—'}`)
                .fontSize(12)
                .fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
    }
    .padding(16)
    .width('100%')
  }

  @Builder
  CalendarTab() {
    Column({ space: 12 }) {
      Text(`日历同步（${this.calendars.length}）`).fontSize(18).fontWeight(FontWeight.Medium)
      Text('方向固定为“灵犀行动 → 外部日历”；绑定与撤销都需要服务端近期认证。')
        .fontSize(12)
        .fontColor('#6D675D')
      List({ space: 8 }) {
        ForEach(this.calendars, (item: CalendarBindingResult) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`${item.provider} · ${item.status}`).fontSize(15)
              Text(`绑定 ${item.bindingId} · 版本 ${item.version}`)
                .fontSize(12)
                .fontColor('#6D675D')
            }
            .alignItems(HorizontalAlign.Start)
            .padding(12)
            .borderRadius(8)
            .backgroundColor('#FFFFFF')
            .width('100%')
          }
        })
      }
      .layoutWeight(1)
      Button('刷新')
        .backgroundColor('#E3EFE9')
        .fontColor('#2F6F5E')
        .onClick(() => {
          this.refreshAll();
        })
    }
    .padding(16)
    .width('100%')
  }
}
