package com.lingxi.identity.domain;

import com.lingxi.identity.api.AgeBand;
import java.time.LocalDate;

public record AgeAssessment(AgeBand ageBand, LocalDate adultTransitionDate) {}
