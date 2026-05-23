<template>
  <div class="send-panel">
    <!-- 头像 -->
    <Avatar :avatar="loginStore.userInfo?.avatar || ''" />

    <div class="input-panel">
      <template v-if="showSend">
        <el-form
          :model="formData"
          :rules="rules"
          ref="formDataRef"
          @submit.prevent
        >
          <!-- 输入框 -->
          <el-form-item class="input-area">
            <el-input
              ref="inputRef"
              clearable
              :placeholder="placeholder"
              v-model="formData.content"
              type="textarea"
              resize="none"
              :maxlength="500"
              :autosize="{ minRows: 1, maxRows: 5 }"
              @input="handleContentInput"
              @keydown.up.prevent="handleMentionUp"
              @keydown.down.prevent="handleMentionDown"
              @keydown.enter="handleEnter"
            />

            <div v-if="showMentionPanel" class="mention-panel">
              <div
                class="mention-item"
                v-for="(user, index) in mentionUserList"
                :key="user.otherUserId"
                :class="{ active: index === mentionActiveIndex }"
                @click="selectMentionUser(user)"
              >
                <Avatar
                  :width="28"
                  :avatar="user.otherAvatar"
                  :userId="user.otherUserId"
                />

                <span class="mention-name">{{ user.otherNickname }}</span>
              </div>

              <div v-if="mentionUserList.length === 0" class="mention-empty">
                No matching users
              </div>
            </div>

            <!-- 图片 -->
            <div v-if="formData.imgPath" class="comment-image">
              <Cover :source="formData.imgPath" />
              <span class="del iconfont icon-close" @click="delImage"></span>
            </div>
          </el-form-item>
        </el-form>

        <!-- 操作栏 -->
        <div class="op-panel">
          <div class="op-btns">
            <!-- 表情 -->
            <el-popover
              ref="elPopoverRef"
              :width="500"
              trigger="click"
              :show-arrow="false"
              placement="bottom-start"
            >
              <template #reference>
                <div class="iconfont icon-emoji"></div>
              </template>

              <template #default>
                <emoji-picker @emoji-click="handleEmojiSelect"></emoji-picker>
              </template>
            </el-popover>

            <div class="iconfont icon-at" @click="openMentionPanel"></div>

            <!-- 上传 -->
            <el-upload
              :show-file-list="false"
              :http-request="selectFile"
              :accept="proxy.imageAccept"
            >
              <div class="iconfont icon-image"></div>
            </el-upload>
          </div>

          <div class="send-btn">
            <el-button type="primary" @click="submitComment">
              Comment
            </el-button>
          </div>
        </div>
      </template>

      <div v-else class="no-send">The creator has disabled comments</div>
    </div>
  </div>
</template>

<script setup>
import "emoji-picker-element";
import { mitter } from "@/eventbus/eventBus.js";
import { uploadImage } from "@/utils/Api";

import { getCurrentInstance, ref, computed, inject } from "vue";

import { useRoute } from "vue-router";
import { useLoginStore } from "@/stores/loginStore";

const handleEmojiSelect = (event) => {
  formData.value.content += event.detail.unicode;
};

const { proxy } = getCurrentInstance();

const videoInfo = inject("videoInfo");

const route = useRoute();
const loginStore = useLoginStore();

const props = defineProps({
  sendType: Number,

  // 新增
  replyData: {
    type: Object,
    default: null,
  },

  showSend: {
    type: Boolean,
    default: true,
  },
});

// ===== 表单 =====
const formData = ref({
  content: "",
  imgPath: null,
});

const mentionUserIds = ref([]);
const mentionUserList = ref([]);
const showMentionPanel = ref(false);
const mentionKeyword = ref("");
const mentionActiveIndex = ref(0);
const lastMentionKeyword = ref("");

const formDataRef = ref();

const rules = {
  content: [
    {
      required: true,
      message: "Please enter a comment",
    },
  ],
};

// ===== placeholder =====
const placeholder = computed(() => {
  return props.replyData?.nickname
    ? `@${props.replyData.nickname}`
    : "Enter a comment...";
});

// ===== 图片 =====
const selectFile = (file) => {
  formData.value.imgPath = file.file;
};

const delImage = () => {
  formData.value.imgPath = null;
};

const loadMentionUserList = async (keyword = "") => {
  const result = await proxy.Request({
    url: proxy.Api.searchMentionUser,
    params: {
      keyword,
    },
  });

  if (!result) return;

  mentionUserList.value = result.data || [];
};

let mentionTimer = null;

const handleContentInput = async () => {
  clearTimeout(mentionTimer);

  const content = formData.value.content || "";
  const match = content.match(/@([^\s@]*)$/);

  if (!match) {
    showMentionPanel.value = false;
    mentionKeyword.value = "";
    mentionUserList.value = [];
    resetMentionActiveIndex();
    return;
  }

  if (!loginStore.userInfo?.userId) {
    showMentionPanel.value = false;
    loginStore.setLogin(true);
    return;
  }

  const currentKeyword = match[1] || "";
  mentionKeyword.value = currentKeyword;

  mentionTimer = setTimeout(async () => {
    const latestContent = formData.value.content || "";
    const latestMatch = latestContent.match(/@([^\s@]*)$/);

    if (!latestMatch) {
      showMentionPanel.value = false;
      mentionKeyword.value = "";
      mentionUserList.value = [];
      resetMentionActiveIndex();
      return;
    }

    if (
      currentKeyword === lastMentionKeyword.value &&
      mentionUserList.value.length > 0
    ) {
      showMentionPanel.value = true;
      return;
    }

    lastMentionKeyword.value = currentKeyword;

    await loadMentionUserList(currentKeyword);

    resetMentionActiveIndex();
    showMentionPanel.value = true;
  }, 500);
};

const selectMentionUser = (user) => {
  clearTimeout(mentionTimer);

  const content = formData.value.content || "";

  if (/@([^\s@]*)$/.test(content)) {
    formData.value.content = content.replace(
      /@([^\s@]*)$/,
      `@${user.otherNickname} `
    );
  } else {
    formData.value.content += `@${user.otherNickname} `;
  }

  if (!mentionUserIds.value.includes(user.otherUserId)) {
    mentionUserIds.value.push(user.otherUserId);
  }

  showMentionPanel.value = false;
  mentionKeyword.value = "";
};

const openMentionPanel = async () => {
  if (!loginStore.userInfo?.userId) {
    loginStore.setLogin(true);
    return;
  }

  await loadMentionUserList("");
  resetMentionActiveIndex();
  showMentionPanel.value = true;
};

const resetMentionActiveIndex = () => {
  mentionActiveIndex.value = 0;
};

const handleMentionUp = () => {
  if (!showMentionPanel.value || mentionUserList.value.length === 0) return;

  mentionActiveIndex.value =
    mentionActiveIndex.value <= 0
      ? mentionUserList.value.length - 1
      : mentionActiveIndex.value - 1;
};

const handleMentionDown = () => {
  if (!showMentionPanel.value || mentionUserList.value.length === 0) return;

  mentionActiveIndex.value =
    mentionActiveIndex.value >= mentionUserList.value.length - 1
      ? 0
      : mentionActiveIndex.value + 1;
};

const handleEnter = (e) => {
  // 没打开 @ 面板，直接放行
  if (!showMentionPanel.value) return;

  // 面板打开但没数据，也放行
  if (mentionUserList.value.length === 0) return;

  // 阻止 textarea 换行
  e.preventDefault();

  const user = mentionUserList.value[mentionActiveIndex.value];

  if (!user) return;

  selectMentionUser(user);
};

// ===== 发送 =====
const submitComment = async () => {
  // 未登录
  if (!loginStore.userInfo?.userId) {
    loginStore.setLogin(true);
    return;
  }

  if (!formData.value.content) {
    proxy.Message.warning("Please enter a comment");
    return;
  }

  let params = {
    videoId: route.params.videoId,
    videoUserId: videoInfo.value?.userId,
    content: formData.value.content,
    imgPath: formData.value.imgPath,
  };

  if (mentionUserIds.value.length > 0) {
    params.mentionUserIds = [...new Set(mentionUserIds.value)].join(",");
  }

  // 回复评论
  if (props.sendType === 1 && props.replyData) {
    params.replyCommentId = props.replyData.replyCommentId;

    params.parentCommentId = props.replyData.parentCommentId;
  }

  // 上传图片
  if (params.imgPath) {
    const imgPath = await uploadImage(params.imgPath, true);

    params.imgPath = imgPath;
  }

  // 发送评论
  const result = await proxy.Request({
    url: proxy.Api.postComment,
    showLoading: true,
    params,
  });

  if (!result) return;

  proxy.Message.success("Commented successfully");

  // 重置
  formData.value = {
    content: "",
    imgPath: null,
  };
  mentionUserIds.value = [];
  mentionUserList.value = [];
  showMentionPanel.value = false;
  mentionKeyword.value = "";

  formDataRef.value?.resetFields();

  // 通知刷新
  mitter.emit("postCommentSuccess", result.data);
};

// ===== 表情 =====
const elPopoverRef = ref();

const activeEmoji = ref("Smileys");

const sendEmoji = (emoji) => {
  formData.value.content += emoji;

  elPopoverRef.value.hide();
};
</script>

<style lang="scss" scoped>
.send-panel {
  margin-top: 16px;
  display: flex;

  .input-panel {
    flex: 1;
    margin-left: 12px;

    .no-send {
      background: #f4f5f7;
      text-align: center;
      padding: 12px;
      border-radius: 6px;
      color: #9499a0;
      font-size: 13px;
    }

    .input-area {
      background: #f4f5f7;
      border-radius: 8px;
      padding: 10px;

      :deep(.el-textarea__inner) {
        background: transparent;
        border: none;
        box-shadow: none;
        padding: 0;
        font-size: 14px;
      }

      .comment-image {
        margin-top: 6px;
        width: 72px;
        height: 72px;
        position: relative;

        .del {
          position: absolute;
          top: 2px;
          right: 2px;
          background: rgba(0, 0, 0, 0.5);
          width: 18px;
          height: 18px;
          border-radius: 50%;
          font-size: 12px;
          cursor: pointer;
        }
      }
    }

    .op-panel {
      display: flex;
      justify-content: space-between;
      margin-top: 6px;

      .op-btns {
        display: flex;

        .iconfont {
          cursor: pointer;
          color: #9499a0;
          padding: 4px 6px;
          border-radius: 4px;
          transition: 0.2s;

          &:hover {
            background: #e3e5e7;
            color: #00aeec;
          }
        }
      }

      .send-btn {
        :deep(.el-button) {
          border-radius: 6px;
          background: #00aeec;
          border: none;
        }
      }
    }
  }
}
.mention-panel {
  margin-top: 6px;
  background: #fff;
  border: 1px solid #e3e5e7;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;

  .mention-item {
    display: flex;
    align-items: center;
    padding: 8px 10px;
    cursor: pointer;

    &:hover {
      background: #f4f5f7;
    }
    &.active {
      background: #e5f6ff;
    }

    .mention-name {
      margin-left: 8px;
      font-size: 14px;
      color: #18191c;
    }
  }

  .mention-empty {
    padding: 10px;
    font-size: 13px;
    color: #9499a0;
  }
}
.mention-btn {
  cursor: pointer;
  color: #9499a0;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 600;

  &:hover {
    background: #e3e5e7;
    color: #00aeec;
  }
}
</style>