<template>
  <div class="message-item">
    <Avatar
      v-if="!isSystemMessage"
      :avatar="extendDto.sendUserAvatar"
      :userId="data.sendUserId"
    />

    <div v-else class="system-avatar">
      <span class="iconfont icon-notice"></span>
    </div>

    <div class="user-info-panel">
      <div class="user-info">
        <router-link
          v-if="!isSystemMessage && data.sendUserId"
          :to="`/user/${data.sendUserId}`"
          class="user-name"
          target="_blank"
        >
          {{ extendDto.sendUserNickname || "User" }}
        </router-link>

        <span v-else class="user-name">System Notification</span>

        <span :class="['title-info', getTitleClass()]">
          {{ convertTitle() }}
        </span>
      </div>

      <!-- 系统消息 / 删除视频 -->
      <div class="comment system-reason" v-if="systemReason">
        {{ systemReason }}
      </div>

      <div
        class="system-success"
        v-if="
          data.messageType === MESSAGE_TYPE.SYS &&
          extendDto.reviewStatus === 3 &&
          data.videoId
        "
      >
      </div>

      <!-- 投币 -->
      <div class="comment" v-if="data.messageType === MESSAGE_TYPE.VIDEO_COIN">
      </div>

      <!-- 评论视频 -->
      <template v-if="data.messageType === MESSAGE_TYPE.VIDEO_COMMENT">
        <div class="comment" v-if="extendDto.messageContent">
          {{ extendDto.messageContent }}
        </div>

        <div class="comment-img" v-if="extendDto.imgPath">
          <img :src="extendDto.imgPath" alt="Video Image" />
        </div>
      </template>

      <!-- 回复评论 -->
      <template v-if="data.messageType === MESSAGE_TYPE.COMMENT_REPLY">
        <div class="comment" v-if="extendDto.messageContent">
          {{ extendDto.messageContent }}
        </div>

        <div class="comment-img" v-if="extendDto.imgPath">
          <img :src="extendDto.imgPath" alt="Comment Image" />
        </div>

        <div class="reply" v-if="extendDto.messageContentReply">
          {{ extendDto.messageContentReply }}
        </div>
      </template>

      <!-- 点赞评论 -->
      <template v-if="data.messageType === MESSAGE_TYPE.COMMENT_LIKE">
        <div class="reply" v-if="extendDto.messageContent">
          {{ extendDto.messageContent }}
        </div>

        <div class="comment-img" v-if="extendDto.imgPath">
          <img :src="extendDto.imgPath" alt="Comment Image" />
        </div>
      </template>

      <!-- @评论 -->
      <template v-if="data.messageType === MESSAGE_TYPE.COMMENT_MENTION">
        <div class="comment" v-if="extendDto.messageContent">
          {{ extendDto.messageContent }}
        </div>

        <div class="comment-img" v-if="extendDto.imgPath">
          <img :src="extendDto.imgPath" alt="Comment Image" />
        </div>

        <div class="reply" v-if="extendDto.messageContentReply">
          {{ extendDto.messageContentReply }}
        </div>
      </template>

      <div class="send-time">
        {{ proxy.Utils.formatDate(data.createdAt) }}
        <span
          class="iconfont icon-delete"
          @click="delMessage(data.messageId)"
        ></span>
      </div>
    </div>

    <!-- 视频相关消息显示封面 -->
    <div class="video-cover" v-if="showVideoCover">
      <router-link :to="`/video/${data.videoId}`" target="_blank">
        <Cover :source="extendDto.videoCover" />

        <div class="video-title" v-if="extendDto.videoTitle">
          {{ extendDto.videoTitle }}
        </div>
      </router-link>
    </div>

    <!-- 关注消息显示用户入口 -->
    <div
      class="follow-entry"
      v-if="data.messageType === MESSAGE_TYPE.USER_FOLLOW && data.sendUserId"
    >
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, h } from "vue";

const { proxy } = getCurrentInstance();

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const emit = defineEmits(["delMessage"]);

const MESSAGE_TYPE = {
  SYS: 1,
  VIDEO_DELETE: 2,
  VIDEO_LIKE: 3,
  VIDEO_SAVE: 4,
  VIDEO_COIN: 5,
  VIDEO_COMMENT: 6,
  COMMENT_REPLY: 7,
  COMMENT_LIKE: 8,
  USER_FOLLOW: 9,
  COMMENT_MENTION: 10,
};

const getTitleClass = () => {
  const ext = extendDto.value || {};

  if (props.data.messageType === MESSAGE_TYPE.SYS) {
    if (ext.reviewStatus === 3) {
      return "title-success";
    }

    if (ext.reviewStatus === 2) {
      return "title-error";
    }
  }

  if (props.data.messageType === MESSAGE_TYPE.VIDEO_DELETE) {
    return "title-error";
  }

  return "";
};

const extendDto = computed(() => {
  try {
    return props.data.extendJson ? JSON.parse(props.data.extendJson) : {};
  } catch (e) {
    return {};
  }
});

const isSystemMessage = computed(() => {
  return [MESSAGE_TYPE.SYS, MESSAGE_TYPE.VIDEO_DELETE].includes(
    props.data.messageType
  );
});

const showVideoCover = computed(() => {
  return (
    [
      MESSAGE_TYPE.SYS,
      MESSAGE_TYPE.VIDEO_DELETE,
      MESSAGE_TYPE.VIDEO_LIKE,
      MESSAGE_TYPE.VIDEO_SAVE,
      MESSAGE_TYPE.VIDEO_COIN,
      MESSAGE_TYPE.VIDEO_COMMENT,
      MESSAGE_TYPE.COMMENT_REPLY,
      MESSAGE_TYPE.COMMENT_LIKE,
      MESSAGE_TYPE.COMMENT_MENTION,
    ].includes(props.data.messageType) && props.data.videoId
  );
});

const systemReason = computed(() => {
  const ext = extendDto.value || {};

  if (props.data.messageType === MESSAGE_TYPE.SYS && ext.reviewStatus === 2) {
    return `Reason: ${ext.messageContent || ""}`;
  }

  if (
    props.data.messageType === MESSAGE_TYPE.VIDEO_DELETE &&
    ext.messageContent
  ) {
    return `Reason for removal: ${ext.messageContent}`;
  }

  return "";
});

const convertTitle = () => {
  const { messageType } = props.data;
  const ext = extendDto.value || {};

  switch (messageType) {
    case MESSAGE_TYPE.SYS:
      if (ext.reviewStatus === 3) {
        return `Video "${ext.videoTitle || "Unknown Video"}" has been approved`;
      }
      if (ext.reviewStatus === 2) {
        return `Video "${ext.videoTitle || "Unknown Video"}" was not approved`;
      }
      return "System Message";

    case MESSAGE_TYPE.VIDEO_DELETE:
      return `Video "${ext.videoTitle || "Unknown Video"}" has been removed`;

    case MESSAGE_TYPE.VIDEO_LIKE:
      return "Liked your video";

    case MESSAGE_TYPE.VIDEO_SAVE:
      return "Saved your video";

    case MESSAGE_TYPE.VIDEO_COIN:
      return `Sent ${ext.coinCount || 1} coins to your video`;

    case MESSAGE_TYPE.VIDEO_COMMENT:
      return "Commented on your video";

    case MESSAGE_TYPE.COMMENT_REPLY:
      return "Replied to your comment";

    case MESSAGE_TYPE.COMMENT_LIKE:
      return "Liked your comment";

    case MESSAGE_TYPE.USER_FOLLOW:
      return "Followed you";

    case MESSAGE_TYPE.COMMENT_MENTION:
      return "Mentioned you in a comment";

    default:
      return "Unknown Message";
  }
};

const delMessage = (messageId) => {
  emit("delMessage", messageId);
};
</script>

<style lang="scss" scoped>
.message-item {
  display: flex;
  padding: 10px 5px;
  border-bottom: 1px solid #ddd;
  align-items: center;

  .system-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background-color: #f2f3f5;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .iconfont {
      font-size: 20px;
      color: var(--text3);
    }
  }

  .user-info-panel {
    margin-left: 10px;
    flex: 1;
    min-width: 0;

    .user-info {
      .user-name {
        font-weight: bold;
        color: var(--text);
        text-decoration: none;
      }

      .title-info {
        margin-left: 5px;
        color: var(--text3);
        font-size: 13px;
      }
    }

    .comment {
      font-size: 13px;
      margin-top: 5px;
      color: var(--text2);
      word-break: break-all;
    }

    .reply {
      border-left: 2px solid #ddd;
      padding-left: 5px;
      font-size: 12px;
      margin-top: 5px;
      color: var(--text3);
      word-break: break-all;
    }

    .comment-img {
      margin-top: 6px;

      img {
        max-width: 120px;
        max-height: 120px;
        border-radius: 4px;
        object-fit: cover;
        cursor: pointer;
        border: 1px solid #eee;
      }
    }

    .send-time {
      margin-top: 5px;
      font-size: 12px;
      color: var(--text3);
    }

    .icon-delete {
      font-size: 14px;
      cursor: pointer;
      margin-left: 10px;

      &:hover {
        color: #ff4d4f;
      }
    }
  }

  .video-cover {
    margin-left: 10px;
    width: 100px;
    flex-shrink: 0;

    a {
      text-decoration: none;
    }

    .video-title {
      margin-top: 4px;
      font-size: 12px;
      color: var(--text2);
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
  }

  .follow-entry {
    margin-left: 10px;
    font-size: 13px;
    flex-shrink: 0;

    a {
      color: var(--blue);
      text-decoration: none;
    }
  }
}

.system-title {
  color: var(--text2);
  font-weight: 600;
}

.system-reason {
  margin-top: 8px;
  color: #ff4d4f;
  font-size: 13px;
  word-break: break-all;
}

.system-success {
  margin-top: 8px;
  font-size: 13px;

  a {
    color: var(--blue);
    text-decoration: none;
  }
}

.title-success {
  color: #67c23a !important;
  font-weight: 600;
}

.title-error {
  color: #f56c6c !important;
  font-weight: 600;
}
</style>