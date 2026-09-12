package com.lingxi.operations.api;

import com.lingxi.kernel.PageResult;

public interface OperationsFacade {
  SupportTicketResult createTicket(CreateSupportTicketCommand command);

  SupportTicketResult getTicket(long userId, long ticketId);

  /** 「我的工单」列表：仅返回当前用户自己的工单，按创建时间倒序分页。 */
  PageResult<SupportTicketResult> listTickets(long userId, int page, int pageSize);

  SupportTicketResult transitionTicket(TransitionTicketCommand command);

  ConfigReleaseResult createRelease(CreateConfigReleaseCommand command);

  ConfigReleaseResult validateRelease(AdminActionCommand command);

  ConfigReleaseResult approveRelease(AdminActionCommand command);

  ConfigReleaseResult startGray(AdminActionCommand command);

  ConfigReleaseResult publishRelease(AdminActionCommand command);

  ConfigReleaseResult rollbackRelease(AdminActionCommand command);
}
