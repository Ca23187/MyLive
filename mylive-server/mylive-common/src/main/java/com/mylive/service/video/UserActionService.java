package com.mylive.service.video;

import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.UserAction;

public interface UserActionService {
    void saveAction(UserAction userAction, TokenInfo tokenInfo);
}
