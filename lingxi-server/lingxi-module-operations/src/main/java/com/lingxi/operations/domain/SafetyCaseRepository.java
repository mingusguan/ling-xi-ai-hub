package com.lingxi.operations.domain;
import java.util.Optional;
public interface SafetyCaseRepository { Optional<SafetyCase> find(long id); boolean update(SafetyCase value,long previousVersion); }
