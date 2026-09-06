package com.lingxi.identity.domain;

import com.lingxi.identity.api.AgeBand;
import com.lingxi.kernel.BusinessException;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** 服务端年龄准入与年龄层判定策略。 */
@Component
public class AgeAccessPolicy {

  private static final int MINIMUM_AGE = 14;
  private static final int ADULT_AGE = 18;

  public AgeAssessment assess(LocalDate verifiedBirthDate, LocalDate today) {
    Objects.requireNonNull(verifiedBirthDate, "verifiedBirthDate");
    Objects.requireNonNull(today, "today");
    if (verifiedBirthDate.isAfter(today)) {
      throw new BusinessException("IDENTITY_INVALID_BIRTH_DATE", "出生日期不合法");
    }
    LocalDate minimumServiceDate = verifiedBirthDate.plusYears(MINIMUM_AGE);
    if (minimumServiceDate.isAfter(today)) {
      return new AgeAssessment(AgeBand.UNDER_14, verifiedBirthDate.plusYears(ADULT_AGE));
    }
    LocalDate adultTransitionDate = verifiedBirthDate.plusYears(ADULT_AGE);
    AgeBand ageBand = adultTransitionDate.isAfter(today) ? AgeBand.TEEN : AgeBand.ADULT;
    return new AgeAssessment(ageBand, adultTransitionDate);
  }
}
