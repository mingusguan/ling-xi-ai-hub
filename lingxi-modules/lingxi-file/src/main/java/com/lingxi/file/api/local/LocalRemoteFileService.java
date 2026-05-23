package com.lingxi.file.api.local;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.file.FileUtils;
import com.lingxi.file.service.ISysFileService;
import com.lingxi.system.api.RemoteFileService;
import com.lingxi.system.api.domain.SysFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 单体模式下的本地文件服务调用。
 */
@Service
public class LocalRemoteFileService implements RemoteFileService {

    private final ISysFileService sysFileService;

    public LocalRemoteFileService(ISysFileService sysFileService) {
        this.sysFileService = sysFileService;
    }

    @Override
    public R<SysFile> upload(MultipartFile file) {
        try {
            String url = sysFileService.uploadFile(file);
            String path = sysFileService.normalizeFileUrl(url);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(path));
            sysFile.setUrl(url);
            sysFile.setPath(path);
            return R.ok(sysFile);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Boolean> delete(String fileUrl) {
        try {
            String checkedFileUrl = StringUtils.substringBefore(fileUrl, "?");
            if (!FileUtils.validateFilePath(checkedFileUrl)) {
                return R.fail(StringUtils.format("资源文件({})非法，不允许删除。", fileUrl));
            }
            sysFileService.deleteFile(fileUrl);
            return R.ok(true);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<String> normalize(String fileUrl) {
        try {
            return R.ok(sysFileService.normalizeFileUrl(fileUrl));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
