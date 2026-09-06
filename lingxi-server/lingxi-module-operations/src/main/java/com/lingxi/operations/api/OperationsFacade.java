package com.lingxi.operations.api;

public interface OperationsFacade {
  SupportTicketResult createTicket(CreateSupportTicketCommand command);

  SupportTicketResult getTicket(long userId, long ticketId);

  SupportTicketResult transitionTicket(TransitionTicketCommand command);

  ConfigReleaseResult createRelease(CreateConfigReleaseCommand command);

  ConfigReleaseResult validateRelease(AdminActionCommand command);

  ConfigReleaseResult approveRelease(AdminActionCommand command);

  ConfigReleaseResult startGray(AdminActionCommand command);

  ConfigReleaseResult publishRelease(AdminActionCommand command);

  ConfigReleaseResult rollbackRelease(AdminActionCommand command);
}
