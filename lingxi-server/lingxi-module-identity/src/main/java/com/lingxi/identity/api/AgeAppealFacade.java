package com.lingxi.identity.api;

import java.time.LocalDate;

/** 14+ 用户年龄申诉入口；证据仅保存受控对象引用。 */
public interface AgeAppealFacade {
  AdminIdentityGovernanceFacade.AgeAppealSummary submit(SubmitAgeAppealCommand command);
  record SubmitAgeAppealCommand(long userId,LocalDate claimedBirthDate,String evidenceRef){}
}
