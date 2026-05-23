<template>
  <div class="comment-item">
    <!-- 头像 -->
    <Avatar
      :width="replyLevel === 1 ? 50 : 30"
      :avatar="data.avatar"
      :userId="data.userId"
    />

    <div class="comment-content-panel">
      <!-- 昵称 -->
      <div class="nick-name-panel">
        <router-link :to="`/user/${data.userId}`" class="nick-name">
          {{ data.nickname }}
        </router-link>

        <!-- UP主标识 -->
        <span v-if="videoInfo.userId === data.userId" class="up-tag">
          Creator
        </span>

        <template v-if="data.replyUserId">
          <div class="reply-title">Reply</div>
          <router-link
            :to="`/user/${data.replyUserId}`"
            class="reply-nick-name"
          >
            @{{ data.replyNickname }}
          </router-link>
        </template>
      </div>

      <!-- 内容 -->
      <div class="comment-message">
        <Tag :type="0" v-if="data.topType === 1" />
        <span v-html="renderCommentContent(data)" />
      </div>

      <!-- 图片 -->
      <div v-if="data.imgPath" class="image-show">
        <Cover
          :source="data.imgPath + proxy.imageThumbnailSuffix"
          :preview="true"
          fit="cover"
        />
      </div>

      <!-- 操作栏 -->
      <div class="comment-op">
        <div class="op-left">
          <div class="comment-time">
            {{ proxy.Utils.formatDate(data.postedAt) }}
          </div>

          <!-- 点赞 -->
          <div
            :class="[
              'iconfont icon-good',
              data.likeCountActive ? 'active' : '',
            ]"
            @click="doLike(data)"
          >
            {{ data.likeCount || "" }}
          </div>

          <!-- 点踩 -->
          <div
            :class="[
              'iconfont icon-no-good',
              data.dislikeCountActive ? 'active' : '',
            ]"
            @click="doHate(data)"
          >
            {{ data.dislikeCount || "" }}
          </div>

          <!-- 回复按钮 -->
          <div class="reply-btn" @click="toggleReply">
            Reply {{ replyLevel === 1 ? `(${data.replyCount})` : "" }}
          </div>
        </div>

        <!-- 更多操作 -->
        <el-dropdown
          v-if="
            data.userId === loginStore.userInfo.userId ||
            videoInfo.userId === loginStore.userInfo.userId
          "
        >
          <span class="op-right iconfont icon-more"></span>

          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-if="
                  videoInfo.userId === loginStore.userInfo.userId &&
                  data.parentCommentId === 0
                "
                @click="topComment"
              >
                {{ data.topType === 1 ? "Unpin" : "Pin" }}
              </el-dropdown-item>

              <el-dropdown-item @click="delComment"> Delete </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- ===== 回复列表 ===== -->
      <div class="reply-list" v-if="replyLevel === 1 && data.showReply">
        <VideoCommentItem
          v-for="item in data.replyList"
          :key="item.commentId"
          :data="item"
          :replyLevel="2"
        />

        <!-- 加载更多 -->
        <div v-if="data.hasMoreReply" class="load-more" @click="loadReplyList">
          Load more replies
        </div>
      </div>

      <!-- 回复输入框 -->
      <VideoCommentSend
        v-if="data.showReply"
        :sendType="1"
        :replyData="currentReplyData"
      />
    </div>
  </div>
</template>

<script setup>
import Tag from "@/components/Tag.vue";
import { doUserAction } from "@/utils/Api";
import { ACTION_TYPE } from "@/utils/Constants.js";
import VideoCommentSend from "./VideoCommentSend.vue";
import VideoCommentItem from "./VideoCommentItem.vue";
import Avatar from "@/components/Avatar.vue";

import { getCurrentInstance, inject, nextTick, computed } from "vue";

import { useRoute } from "vue-router";
import { useLoginStore } from "@/stores/loginStore";
import { mitter } from "@/eventbus/eventBus.js";

const { proxy } = getCurrentInstance();
const route = useRoute();
const loginStore = useLoginStore();

const props = defineProps({
  data: Object,
  replyLevel: {
    type: Number,
    default: 1,
  },
});

const videoInfo = inject("videoInfo");

const currentReplyData = computed(() => {
  return {
    replyCommentId: props.data.commentId,

    parentCommentId:
      props.replyLevel === 1
        ? props.data.commentId
        : props.data.parentCommentId,

    nickname: props.data.nickname,
  };
});

// ===== 切换回复 + 加载 =====
const toggleReply = async () => {
  props.data.showReply = !props.data.showReply;

  // 只有一级评论才加载回复列表
  if (
    props.replyLevel === 1 &&
    props.data.showReply &&
    props.data.replyList.length === 0
  ) {
    await loadReplyList();
  }
};

// ===== 加载回复 =====
const loadReplyList = async () => {
  if (props.data.replyLoading || !props.data.hasMoreReply) return;

  props.data.replyLoading = true;

  const result = await proxy.Request({
    url: proxy.Api.loadReply,
    params: {
      parentId:
        props.replyLevel === 1
          ? props.data.commentId
          : props.data.parentCommentId,
      pageNo: props.data.replyPageNo,
    },
  });

  props.data.replyLoading = false;
  if (!result) return;

  const page = result.data.commentData;

  if (props.data.replyPageNo === 1) {
    props.data.replyList = page.list.map((item) => ({
      ...item,
      replyList: [],
      replyPageNo: 1,
      replyLoading: false,
      showReply: false,
      hasMoreReply: true,
    }));
  } else {
    props.data.replyList = props.data.replyList.concat(page.list);
  }

  props.data.hasMoreReply = page.pageNo < page.pageTotal;

  props.data.replyPageNo++;
};

// ===== 点赞 =====
const doLike = (data) => {
  doUserAction(
    {
      videoId: route.params.videoId,
      actionType: ACTION_TYPE.COMMENT_LIKE.value,
      commentId: data.commentId,
    },
    () => {
      if (data.dislikeCountActive) {
        data.dislikeCountActive = false;
        data.dislikeCount--;
      }

      if (data.likeCountActive) {
        data.likeCountActive = false;
        data.likeCount--;
      } else {
        data.likeCount++;
        data.likeCountActive = true;
      }
    }
  );
};

// ===== 点踩 =====
const doHate = (data) => {
  doUserAction(
    {
      videoId: route.params.videoId,
      actionType: ACTION_TYPE.COMMENT_HATE.value,
      commentId: data.commentId,
    },
    () => {
      if (data.likeCountActive) {
        data.likeCountActive = false;
        data.likeCount--;
      }

      if (data.dislikeCountActive) {
        data.dislikeCountActive = false;
        data.dislikeCount--;
      } else {
        data.dislikeCount++;
        data.dislikeCountActive = true;
      }
    }
  );
};

// ===== 删除 =====
const delComment = () => {
  proxy.Confirm({
    message: "Are you sure you want to delete this comment?",
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.userDelComment,
        params: {
          commentId: props.data.commentId,
        },
      });

      if (!result) return;

      mitter.emit("delCommentCallback", {
        parentCommentId: props.data.parentCommentId,
        commentId: props.data.commentId,
      });
    },
  });
};

// ===== 置顶 =====
const topComment = () => {
  proxy.Confirm({
    message: `Are you sure you want to ${
      props.data.topType === 1 ? "unpin" : "pin"
    } this comment?`,
    okfun: async () => {
      const result = await proxy.Request({
        url:
          props.data.topType === 1
            ? proxy.Api.userCancelTopComment
            : proxy.Api.userTopComment,
        params: {
          commentId: props.data.commentId,
        },
      });

      if (!result) return;

      mitter.emit("topCommentCallback", {
        commentId: props.data.commentId,
        topType: props.data.topType === 1 ? 0 : 1, // 切换后的状态
      });
    },
  });
};

const renderCommentContent = (data) => {
  let content = proxy.Utils.resetHtmlContent(data.content || "");

  if (!data.mentionUsers || data.mentionUsers.length === 0) {
    return content;
  }

  data.mentionUsers.forEach((user) => {
    const reg = new RegExp(`@${escapeRegExp(user.nickname)}`, "g");

    content = content.replace(
      reg,
      `<a class="comment-mention" href="/user/${user.userId}" target="_blank" rel="noopener noreferrer">@${user.nickname}</a>`
    );
  });

  return content;
};

const escapeRegExp = (str) => {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
};
</script>

<style lang="scss" scoped>
.comment-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f1f2f3;

  .comment-content-panel {
    flex: 1;
    margin-left: 12px;

    .nick-name-panel {
      font-size: 14px;
      display: flex;
      align-items: center;

      .nick-name {
        color: #61666d;
        font-weight: 500;
        text-decoration: none;

        &:hover {
          color: #00aeec;
        }
      }

      .reply-title {
        margin: 0 4px;
        color: #9499a0;
      }

      .reply-nick-name {
        color: #00aeec;
      }
    }

    .comment-message {
      font-size: 14px;
      margin-top: 4px;
      line-height: 1.6;
      color: #18191c;
    }

    .image-show {
      margin-top: 6px;
      width: 100px;
      border-radius: 6px;
      overflow: hidden;
    }

    .comment-op {
      margin-top: 6px;
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      color: #9499a0;

      .op-left {
        display: flex;
        align-items: center;

        .comment-time {
          margin-right: 16px;
        }

        .iconfont {
          margin-right: 16px;
          cursor: pointer;
          transition: 0.2s;

          &:hover {
            color: #00aeec;
          }
        }

        .active::before {
          color: #00aeec;
        }

        .reply-btn {
          cursor: pointer;

          &:hover {
            color: #00aeec;
          }
        }
      }

      .op-right {
        cursor: pointer;
      }
    }
  }
}

.reply-list {
  margin-top: 8px;
  padding-left: 42px;
  border-left: 2px solid #f6f7f8;
}

.load-more {
  font-size: 13px;
  color: #9499a0;
  cursor: pointer;
  margin-top: 6px;

  &:hover {
    color: #00aeec;
  }
}
.up-tag {
  margin-left: 6px;
  padding: 0 4px;
  font-size: 12px;
  color: #fff;
  background-color: #fb7299; // B站粉色
  border-radius: 4px;
  line-height: 16px;
}

.comment-message {
  :deep(.comment-mention) {
    color: #00aeec;
    text-decoration: none;
    cursor: pointer;

    &:hover {
      color: #00a1d6;
    }
  }
}
</style>