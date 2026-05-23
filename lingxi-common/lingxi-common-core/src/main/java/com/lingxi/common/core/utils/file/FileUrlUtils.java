package com.lingxi.common.core.utils.file;

import com.lingxi.common.core.utils.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 文件访问地址转换工具。
 */
public class FileUrlUtils
{
    private static final String PREVIEW_PREFIX = "/file/preview/";

    private FileUrlUtils()
    {
    }

    /**
     * 将上传接口返回的可访问地址转换为业务表保存地址。
     */
    public static String normalizeForStorage(String fileUrl)
    {
        if (StringUtils.isBlank(fileUrl))
        {
            return fileUrl;
        }
        String normalized = Arrays.stream(fileUrl.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(FileUrlUtils::normalizeSingleForStorage)
                .collect(Collectors.joining(","));
        return StringUtils.isBlank(normalized) ? fileUrl : normalized;
    }

    /**
     * 将稳定保存地址转换为当前系统可访问的预览地址。
     */
    public static String toPreviewPath(String fileUrl)
    {
        if (StringUtils.isBlank(fileUrl))
        {
            return fileUrl;
        }
        String normalized = normalizeSingleForStorage(fileUrl);
        if (StringUtils.startsWith(normalized, PREVIEW_PREFIX))
        {
            return normalized;
        }
        return PREVIEW_PREFIX + trimStartSlash(normalized);
    }

    private static String normalizeSingleForStorage(String fileUrl)
    {
        String cleanUrl = StringUtils.substringBefore(fileUrl, "?");
        if (StringUtils.contains(cleanUrl, PREVIEW_PREFIX))
        {
            return PREVIEW_PREFIX + trimStartSlash(StringUtils.substringAfter(cleanUrl, PREVIEW_PREFIX));
        }
        if (StringUtils.startsWith(cleanUrl, "/"))
        {
            return cleanUrl;
        }
        if (!StringUtils.contains(cleanUrl, "://"))
        {
            return cleanUrl;
        }
        String uriPath = parseUriPath(cleanUrl);
        if (StringUtils.isNotBlank(uriPath))
        {
            return uriPath;
        }
        return cleanUrl;
    }

    private static String parseUriPath(String fileUrl)
    {
        try
        {
            URI uri = URI.create(fileUrl);
            if (StringUtils.isNotBlank(uri.getPath()))
            {
                return URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8);
            }
        }
        catch (Exception ignored)
        {
            return StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }

    private static String trimStartSlash(String value)
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
