package com.lingxi.operations.interfaces;

import com.lingxi.kernel.*;
import com.lingxi.operations.api.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/support-tickets")
public class SupportController {
  private final OperationsFacade facade;

  public SupportController(OperationsFacade f) {
    facade = f;
  }

  @PostMapping
  public ApiResponse<SupportTicketResult> create(@RequestBody Body b, HttpServletRequest r) {
    return ok(
        facade.createTicket(
            new CreateSupportTicketCommand(
                user(), b.category(), b.subject(), b.description(), b.priority())),
        r);
  }

  @GetMapping("/{id}")
  public ApiResponse<SupportTicketResult> get(@PathVariable long id, HttpServletRequest r) {
    return ok(facade.getTicket(user(), id), r);
  }

  private long user() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T v, HttpServletRequest r) {
    return ApiResponse.success(v, String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record Body(String category, String subject, String description, String priority) {}
}
