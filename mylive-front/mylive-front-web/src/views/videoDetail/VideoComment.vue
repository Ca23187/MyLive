<template>
  <div class="comment-panel" ref="commentRef">
    <div class="comment-title">
      <div class="title">
        Comments<span class="comment-count">{{ dataSource.totalCount }}</span>
      </div>

      <div
        :class="['order-type-item', orderType === 0 ? 'active' : '']"
        @click="changeOrder(0)"
      >
        Top
      </div>

      <el-divider direction="vertical" />

      <div
        :class="['order-type-item', orderType === 1 ? 'active' : '']"
        @click="changeOrder(1)"
      >
        Newest
      </div>
    </div>

    <div class="comment-content-panel">
      <!-- 发布评论 -->
      <VideoCommentSend v-if="showComment" :sendType="0" />

      <div v-else class="comment-disabled">Comments are disabled</div>

      <!-- 评论列表 -->
      <div class="comment-list">
        <DataLoadMoreList
          :dataSource="dataSource"
          :loading="loadingData"
          @loadData="loadCommentList"
          layoutType="list"
          loadEndMsg="No more comments"
        >
          <template #default="{ data }">
            <VideoCommentItem :data="data" />
          </template>
        </DataLoadMoreList>
      </div>
    </div>
  </div>
</template>

<script setup>
import { mitter } from "@/eventbus/eventBus.js";
import VideoCommentItem from "./VideoCommentItem.vue";
import VideoCommentSend from "./VideoCommentSend.vue";
import {
  computed,
  getCurrentInstance,
  inject,
  onMounted,
  onUnmounted,
  ref,
} from "vue";
import { useRoute } from "vue-router";

const { proxy } = getCurrentInstance();
const route = useRoute();

// ===== 视频信息 =====
const videoInfo = inject("videoInfo");

// 是否允许评论
const showComment = computed(() => {
  return videoInfo.value?.allowComment !== 0;
});

// ===== 状态 =====
const loadingData = ref(false);
const dataSource = ref({
  list: [],
  pageNo: 1,
  totalCount: 0,
  hasMore: true,
});
const orderType = ref(0);

// ===== 切换排序 =====
const changeOrder = async (_orderType) => {
  if (loadingData.value) return; // ✅ 防抖
  orderType.value = _orderType;

  dataSource.value.pageNo = 1;
  dataSource.value.hasMore = true; // ✅ 关键：重置

  await loadCommentList(true);
};

// ===== 初始化评论结构 =====
const initComment = (item) => {
  return {
    ...item,
    replyList: [],
    replyPageNo: 1,
    replyLoading: false,
    showReply: false,
    hasMoreReply: true,
  };
};

// ===== 加载评论 =====
const loadCommentList = async (isReset = false) => {
  if (loadingData.value || (!dataSource.value.hasMore && !isReset)) return;

  loadingData.value = true;

  const result = await proxy.Request({
    url: proxy.Api.loadComment,
    params: {
      videoId: route.params.videoId,
      pageNo: dataSource.value.pageNo, // ✅ 修复
      orderType: orderType.value,
    },
  });

  loadingData.value = false;
  if (!result) return;

  const commentData = result.data.commentData;
  const newList = commentData.list.map(initComment);

  // ✅ 分页逻辑
  if (dataSource.value.pageNo === 1) {
    dataSource.value.list = newList;
  } else {
    dataSource.value.list = dataSource.value.list.concat(newList);
  }

  // ✅ 页码递增
  dataSource.value.pageNo++;

  dataSource.value.totalCount = commentData.totalCount;

  // ✅ 是否还有数据
  dataSource.value.hasMore = newList.length > 0;
};

const commentRef = ref(null);
const hasLoaded = ref(false); // 防止重复加载
let observer = null;

// ===== 发布评论成功 =====
onMounted(() => {
  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0];

      if (entry.isIntersecting && !hasLoaded.value) {
        hasLoaded.value = true;
        loadCommentList();
        observer.disconnect();
      }
    },
    {
      root: null,
      threshold: 0.1,
    }
  );

  if (commentRef.value) {
    observer.observe(commentRef.value);
  }

  mitter.on("postCommentSuccess", (comment) => {
    // 一级评论
    if (!comment.parentCommentId) {
      dataSource.value.list.unshift(initComment(comment));
      dataSource.value.totalCount++;
      return;
    }

    // 二级评论
    const parent = dataSource.value.list.find(
      (item) => item.commentId === comment.parentCommentId
    );

    if (!parent) return;

    // 只有展开才插入
    if (parent.showReply) {
      parent.replyList.unshift(initComment(comment));
    }

    parent.replyCount++;
  });

  // ===== 删除评论 =====
  mitter.on("delCommentCallback", ({ parentCommentId, commentId }) => {
    if (parentCommentId === 0) {
      dataSource.value.list = dataSource.value.list.filter(
        (item) => item.commentId !== commentId
      );
      dataSource.value.totalCount--;
      return;
    }

    const parent = dataSource.value.list.find(
      (item) => item.commentId === parentCommentId
    );

    if (!parent) return;

    parent.replyList = parent.replyList.filter(
      (item) => item.commentId !== commentId
    );

    parent.replyCount--;
  });

  // ===== 置顶刷新 =====
  mitter.on("topCommentCallback", ({ commentId, topType }) => {
    const list = dataSource.value.list;

    // 1️⃣ 如果是取消置顶
    if (topType === 0) {
      const item = list.find((item) => item.commentId === commentId);
      if (item) {
        item.topType = 0;
      }
      return;
    }

    // 2️⃣ 如果是设置置顶
    // 清掉其他
    list.forEach((item) => {
      item.topType = 0;
    });

    const index = list.findIndex((item) => item.commentId === commentId);

    if (index === -1) return;

    const item = list[index];
    item.topType = 1;

    // 移动到最前
    list.splice(index, 1);
    list.unshift(item);
  });
});

// ===== 销毁 =====
onUnmounted(() => {
  if (observer) {
    observer.disconnect();
  }
  mitter.off("postCommentSuccess");
  mitter.off("delCommentCallback");
  mitter.off("topCommentCallback");
});
</script>

<style lang="scss" scoped>
.comment-panel {
  margin-top: 20px;

  .comment-title {
    display: flex;
    align-items: center;
    font-size: 14px;
    margin-bottom: 10px;

    .title {
      font-size: 18px;
      font-weight: 600;
      margin-right: 20px;

      .comment-count {
        margin-left: 6px;
        font-size: 13px;
        color: #9499a0;
      }
    }

    .order-type-item {
      margin-right: 12px;
      cursor: pointer;
      color: #9499a0;
      transition: all 0.2s;

      &:hover {
        color: #00aeec;
      }
    }

    .active {
      color: #00aeec;
      font-weight: 500;
    }
  }

  .comment-content-panel {
    padding-left: 0;

    .comment-list {
      padding-bottom: 20px;
    }
  }
}

.comment-disabled {
  margin: 20px 0;
  padding: 20px;
  text-align: center;
  color: #9499a0;
  font-size: 14px;
  background-color: #f6f7f8;
  border-radius: 6px;
}
</style>