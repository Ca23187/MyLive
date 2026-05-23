<template>
  <div class="video-tab">
    <div class="tab">Videos</div>
    <div class="search">
      <el-input
        clearable
        placeholder="Search videos"
        size="small"
        v-model="videoTitleFuzzy"
        @keyup.enter="loadVideoPostList"
      >
        <template #suffix>
          <span class="iconfont icon-search"></span>
        </template>
      </el-input>
    </div>
  </div>
  <div class="video-manage">
    <div class="top-info">
      <div class="all-video-panel">
        <div class="all-video" @click="cleanStatusLoad">
          All Videos<span class="count-info">{{
            countInfo.inProgress +
            countInfo.reviewPassedCount +
            countInfo.reviewRejectedCount
          }}</span>
        </div>
      </div>
      <div class="video-status">
        <span
          :class="['item', status == -1 ? 'active' : '']"
          @click="statusLoad(-1)"
          >Processing<span class="count-info">{{
            countInfo.inProgress
          }}</span></span
        >
        <el-divider direction="vertical" />
        <span
          :class="['item', status == 3 ? 'active' : '']"
          @click="statusLoad(3)"
          >Approved<span class="count-info">{{
            countInfo.reviewPassedCount
          }}</span></span
        >
        <el-divider direction="vertical" />
        <span
          :class="['item', status == 4 ? 'active' : '']"
          @click="statusLoad(4)"
          >Rejected<span class="count-info">{{
            countInfo.reviewRejectedCount
          }}</span></span
        >
      </div>
    </div>
    <div class="video-list">
      <DataList :dataSource="dataSource" @loadData="loadVideoPostList">
        <template #default="{ data }">
          <VideoItem :data="data" @reload="loadVideoPostList"></VideoItem>
        </template>
      </DataList>
    </div>
  </div>
</template>

<script setup>
import VideoItem from "./VideoItem.vue";
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUnmounted,
} from "vue";
const { proxy } = getCurrentInstance();
import { useRoute, useRouter } from "vue-router";
const route = useRoute();
const router = useRouter();

const videoTitleFuzzy = ref();
const status = ref();
const statusLoad = (_status) => {
  status.value = _status;
  loadVideoPostList();
};
const cleanStatusLoad = () => {
  status.value = null;
  loadVideoPostList();
};

const dataSource = ref({});
const loadVideoPostList = async () => {
  let params = {
    pageNo: dataSource.value.pageNo,
    videoTitleFuzzy: videoTitleFuzzy.value,
    status: status.value,
  };

  let result = await proxy.Request({
    url: proxy.Api.loadUcenterVideoPostList,
    params,
  });
  if (!result) {
    return;
  }
  dataSource.value = result.data;
  if (hasTranscodingVideo()) {
    startTimer();
  } else {
    cleanTimer();
  }
};

const countInfo = ref({ inProgress: 0, reviewPassedCount: 0, reviewRejectedCount: 0 });
const loadCountInfo = async () => {
  let result = await proxy.Request({
    url: proxy.Api.getUcenterVideoCountInfo,
  });
  if (!result) {
    return;
  }
  countInfo.value = result.data;
};
let timmer = ref(null);
const startTimer = () => {
  if (timmer.value !== null) return;

  timmer.value = setInterval(async () => {
    await loadVideoPostList();
    await loadCountInfo();

    if (!hasTranscodingVideo()) {
      cleanTimer();
    }
  }, 10000);
};

const cleanTimer = () => {
  if (timmer.value !== null) {
    clearInterval(timmer.value);
    timmer.value = null;
  }
};

const hasTranscodingVideo = () => {
  return dataSource.value?.list?.some((item) => item.status === 0);
};

onMounted(() => {
  loadVideoPostList();
  loadCountInfo();
});

onUnmounted(() => {
  cleanTimer();
});
</script>

<style lang="scss" scoped>
.video-tab {
  border-bottom: 1px solid #ddd;
  display: flex;
  padding: 0px 40px;
  justify-content: space-between;
  .tab {
    cursor: pointer;
    font-weight: bold;
    font-size: 16px;
    color: var(--blue);
    padding-bottom: 15px;
    border-bottom: 3px solid var(--blue);
  }
  .search {
    width: 200px;
  }
}
.video-manage {
  margin-top: 10px;
  padding: 0px 40px 10px 40px;
  .top-info {
    .count-info {
      padding: 0px 5px;
    }
    .all-video-panel {
      display: flex;
      .all-video {
        cursor: pointer;
        font-size: 14px;
        color: var(--blue);
      }
    }
    .video-status {
      margin-top: 10px;
      display: flex;
      align-items: center;
      .item {
        cursor: pointer;
        font-size: 13px;
        margin-right: 0px;
        color: var(--text2);
        &:hover {
          color: var(--blue);
        }
      }
      .active {
        color: var(--blue);
      }
    }
  }
}
</style>
