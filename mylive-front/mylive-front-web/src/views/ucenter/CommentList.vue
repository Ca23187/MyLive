<template>
  <div class="comment-panel">
    <VideoSearchSelect @loadData="loadData4VideoSelect"></VideoSearchSelect>
    <Table
      ref="tableInfoRef"
      :columns="columns"
      :fetch="loadDataList"
      :dataSource="tableData"
      :options="tableOptions"
      :extHeight="tableOptions.extHeight"
    >
      <template #slotComment="{ index, row }">
        <div class="comment-info">
          <Avatar :avatar="row.avatar" :userId="row.userId"></Avatar>
          <div class="comment">
            <div>
              <router-link
                target="_blank"
                class="a-link nick-name"
                :to="`/user/${row.userId}`"
                >{{ row.nickname }}
              </router-link>
              <template v-if="row.replyUserId">
                Replied to @
                <router-link
                  target="_blank"
                  class="a-link nick-name"
                  :to="`/user/${row.replyUserId}`"
                  >{{ row.replyNickname }} </router-link
                >'s comment
              </template>
            </div>

            <div class="content" v-html="renderCommentContent(row)"></div>
            <div v-if="row.imgPath" class="image-show">
              <Cover
                :source="row.imgPath + proxy.imageThumbnailSuffix"
                :preview="true"
                fit="cover"
              ></Cover>
            </div>
            <div class="time-info">
              <div class="time">{{ row.postedAt }}</div>
              <div
                class="iconfont icon-delete"
                @click="delComment(row.commentId)"
              ></div>
            </div>
          </div>
        </div>
      </template>

      <template #slotVideo="{ index, row }">
        <router-link
          :to="`/video/${row.videoId}`"
          target="_blank"
          class="a-link"
        >
          <Cover :source="row.videoCover"></Cover>
          <div class="video-name">{{ row.videoTitle }}</div>
        </router-link>
      </template>
    </Table>
  </div>
</template>

<script setup>
import VideoSearchSelect from "./VideoSerchSelect.vue";
import Table from "@/components/Table.vue";
import { ref, reactive, getCurrentInstance, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const currentVideoId = ref(route.query.videoId);
const loadData4VideoSelect = (videoId) => {
  currentVideoId.value = videoId;
  loadDataList();
};

const columns = [
  {
    label: "Comment",
    scopedSlots: "slotComment",
  },
  {
    label: "Video",
    scopedSlots: "slotVideo",
    width: 150,
  },
];

const tableInfoRef = ref();
const tableOptions = ref({
  extHeight: 10,
});

const tableData = ref({});
const loadDataList = async (videoId) => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    videoId: currentVideoId.value,
  };
  let result = await proxy.Request({
    url: proxy.Api.ucLoadComment,
    params: params,
  });
  if (!result) {
    return;
  }
  Object.assign(tableData.value, result.data);
};

const delComment = (commentId) => {
  proxy.Confirm({
    message: "Are you sure you want to delete this comment?",
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.ucDelComment,
        params: {
          commentId
        },
      });
      if (!result) {
        return;
      }
      proxy.Message.success("Deleted successfully");
      loadDataList();
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
.comment-panel {
  .comment-info {
    display: flex;
    .comment {
      margin-left: 10px;
    }
    .time-info {
      display: flex;
      font-size: 12px;
      .iconfont {
        margin-left: 5px;
        font-size: 13px;
        cursor: pointer;
      }
    }
  }
  .video-name {
    text-decoration: none;
    color: var(--text3);
    font-size: 13px;
    margin-top: 5px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.image-show {
  width: 100px;
  height: 100px;
  overflow: hidden;
  margin: 5px 0px;
}
.content {
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
