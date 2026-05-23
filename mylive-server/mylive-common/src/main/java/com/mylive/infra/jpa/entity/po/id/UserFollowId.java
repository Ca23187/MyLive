package com.mylive.infra.jpa.entity.po.id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserFollowId {
    private Long userId;
    private Long followUserId;
}
