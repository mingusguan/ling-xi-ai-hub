import { AchievementResult, GoalResult, NotificationResult, OccurrenceResult } from '../model/Types';
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
    } catch (error) {
      this.message = `数据加载失败：${JSON.stringify(error)}`;
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
        this.AchievementsTab()
      }.tabBar('成就')

      TabContent() {
        this.MessagesTab()
      }.tabBar('消息')
    }
    .width('100%')
    .height('100%')
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
}
