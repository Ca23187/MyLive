package com.mylive.service.file.transcode;

import com.mylive.infra.jpa.entity.dto.TranscodeResult;

public interface FileTranscodeTxService {

    void updateSuccess(String fileId, TranscodeResult result);

    void updateFail(String fileId);
}
