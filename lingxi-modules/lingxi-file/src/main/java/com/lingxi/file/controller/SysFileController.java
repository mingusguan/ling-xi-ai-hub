package com.lingxi.file.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.multipart.MultipartFile;
import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.file.FileUtils;
import com.lingxi.common.security.annotation.RequiresLogin;
import com.lingxi.file.service.ISysFileService;
import com.lingxi.system.api.domain.SysFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 文件请求处理
 * 
 * @author cloud
 */
@RestController
@RequestMapping("/file")
public class SysFileController
{
    private static final Logger log = LoggerFactory.getLogger(SysFileController.class);

    @Autowired
    private ISysFileService sysFileService;

    /**
     * 文件上传请求
     */
    @PostMapping("upload")
    public R<SysFile> upload(MultipartFile file)
    {
        try
        {
            // 上传并返回访问地址
            String url = sysFileService.uploadFile(file);
            String path = sysFileService.normalizeFileUrl(url);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(path));
            sysFile.setUrl(url);
            sysFile.setPath(path);
            return R.ok(sysFile);
        }
        catch (Exception e)
        {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 私有文件预览请求
     */
    @GetMapping("normalize")
    public R<String> normalize(@RequestParam("fileUrl") String fileUrl)
    {
        return R.ok(sysFileService.normalizeFileUrl(fileUrl));
    }

    @RequiresLogin
    @GetMapping("/preview/**")
    public void preview(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String objectKey = resolvePreviewObjectKey(request);
        String checkedObjectKey = StringUtils.substringBefore(objectKey, "?");
        if (!FileUtils.validateFilePath(checkedObjectKey))
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "资源文件非法，不允许访问。");
            return;
        }
        try
        {
            String url = sysFileService.getAccessibleUrl(objectKey);
            response.sendRedirect(url);
        }
        catch (Exception e)
        {
            log.error("生成私有文件访问地址失败，objectKey={}", objectKey, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "生成文件访问地址失败。");
        }
    }

    /**
     * 文件删除请求
     */
    @DeleteMapping("delete")
    public R<Boolean> delete(String fileUrl)
    {
        try
        {
            String checkedFileUrl = StringUtils.substringBefore(fileUrl, "?");
            if (!FileUtils.validateFilePath(checkedFileUrl))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
            }
            sysFileService.deleteFile(fileUrl);
            return R.ok();
        }
        catch (Exception e)
        {
            log.error("删除文件失败", e);
            return R.fail(e.getMessage());
        }
    }

    private String resolvePreviewObjectKey(HttpServletRequest request)
    {
        String bestMatchingPattern = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        String pathWithinMapping = String.valueOf(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE));
        String objectKey = StringUtils.substringAfter(pathWithinMapping, bestMatchingPattern.replace("/**", ""));
        return trimStartSlash(objectKey);
    }

    private String trimStartSlash(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        while (value.startsWith("/"))
        {
            value = value.substring(1);
        }
        return value;
    }
}
