package com.lingxi.content.interfaces;

import com.lingxi.content.api.*;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/** PC Web 与 HarmonyOS 共用的内容资产接口。 */
@RestController
@RequestMapping("/api/v1")
public class ContentController {
  private final ContentFacade facade;

  public ContentController(ContentFacade f) {
    facade = f;
  }

  @PostMapping("/files/upload-tickets")
  public ApiResponse<UploadTicketResult> ticket(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody UploadBody b,
      HttpServletRequest r) {
    return ok(
        facade.createUploadTicket(
            new CreateUploadTicketCommand(
                key,
                user(),
                b.purpose(),
                b.originalName(),
                b.sizeBytes(),
                b.mimeType(),
                b.contentHash(),
                b.sensitivity())),
        r);
  }

  @PostMapping("/files/{id}/uploaded")
  public ApiResponse<FileResult> uploaded(
      @PathVariable long id, @RequestBody VersionBody b, HttpServletRequest r) {
    return ok(facade.markUploaded(user(), id, b.expectedVersion()), r);
  }

  @PostMapping("/files/{id}/references")
  public ApiResponse<FileResult> reference(
      @PathVariable long id, @RequestBody ReferenceBody b, HttpServletRequest r) {
    return ok(
        facade.referenceReadyFile(
            new ReferenceFileCommand(user(), id, b.resourceType(), b.resourceId())),
        r);
  }

  @GetMapping("/files")
  public ApiResponse<PageResult<FileResult>> myFiles(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest r) {
    return ok(facade.listMyFiles(user(), page, pageSize), r);
  }

  @GetMapping("/files/{id}")
  public ApiResponse<FileResult> file(@PathVariable long id, HttpServletRequest r) {
    return ok(facade.getReadyFile(user(), id), r);
  }

  @DeleteMapping("/files/{id}/references")
  public ApiResponse<FileResult> removeReference(
      @PathVariable long id, @RequestBody ReferenceBody body, HttpServletRequest request) {
    return ok(facade.removeReference(user(), id, body.resourceType(), body.resourceId()), request);
  }

  @DeleteMapping("/files/{id}")
  public ApiResponse<FileResult> deleteFile(
      @PathVariable long id, @RequestParam long expectedVersion, HttpServletRequest request) {
    return ok(facade.deleteFile(user(), id, expectedVersion), request);
  }

  @PostMapping("/import-jobs")
  public ApiResponse<TransferJobResult> importJob(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody ImportBody b,
      HttpServletRequest r) {
    return ok(
        facade.createImport(new CreateImportJobCommand(key, user(), b.sourceFileId(), b.format())),
        r);
  }

  @PostMapping("/import-jobs/{id}/confirm")
  public ApiResponse<TransferJobResult> confirm(
      @PathVariable long id, @RequestBody VersionBody b, HttpServletRequest r) {
    return ok(facade.confirmImport(user(), id, b.expectedVersion()), r);
  }

  @PostMapping("/export-jobs")
  public ApiResponse<TransferJobResult> export(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody ExportBody b,
      HttpServletRequest r) {
    return ok(
        facade.createExport(
            new CreateExportJobCommand(
                key,
                user(),
                b.scopeJson(),
                b.format(),
                ActorContextHolder.requireUser().recentAuthentication())),
        r);
  }

  @GetMapping("/transfer-jobs/{id}")
  public ApiResponse<TransferJobResult> job(@PathVariable long id, HttpServletRequest r) {
    return ok(facade.getTransfer(user(), id), r);
  }

  @GetMapping("/templates")
  public ApiResponse<List<TemplateVersionResult>> templates(HttpServletRequest r) {
    return ok(facade.listPublishedTemplates(user()), r);
  }

  private long user() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T v, HttpServletRequest r) {
    return ApiResponse.success(v, String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record ReferenceBody(String resourceType, String resourceId) {}

  public record UploadBody(
      String purpose,
      String originalName,
      long sizeBytes,
      String mimeType,
      String contentHash,
      String sensitivity) {}

  public record ImportBody(long sourceFileId, String format) {}

  public record ExportBody(String scopeJson, String format) {}

  public record VersionBody(long expectedVersion) {}
}
