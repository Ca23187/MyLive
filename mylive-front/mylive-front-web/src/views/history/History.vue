<template>
  <div class="history-list">
    <div class="top-info">
      <div class="iconfont icon-wating">Watch History</div>
      <div>
        <el-button type="primary" @click="cleanAll">Clear History</el-button>
      </div>
    </div>

    <el-timeline>
      <div class="data-list">
        <DataLoadMoreList
          :dataSource="dataSource"
          :loading="loadingData"
          @loadData="loadDataList"
          layoutType="line"
        >
          <template #default="{ data }">
            <el-timeline-item
              :timestamp="proxy.Utils.formatDate(data.lastPlayedAt)"
              placement="top"
            >
              <div
                class="history-item"
                style="
                  width: 100%;
                  display: flex;
                  align-items: center;
                  gap: 16px;
                  padding: 12px 18px 12px 12px;
                  box-sizing: border-box;
                  background: #fff;
                  border-radius: 8px;
                "
              >
                <div class="history-cover" @click="goDetail(data.videoId)">
<Cover
  :source="data.videoCover"
  :width="200"
  :scale="0.56"
  fit="cover"
  show404WhenEmpty
/>

                  <div v-if="data.duration > 0" class="progress-bar">
                    <div
                      class="progress-inner"
                      :style="{ width: getProgressPercent(data) + '%' }"
                    ></div>
                  </div>
                </div>

                <div
                  class="video-info"
                  style="
                    flex: 1;
                    min-width: 0;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                  "
                >
                  <div
                    class="video-title"
                    @click="goDetail(data.videoId)"
                    style="
                      cursor: pointer;
                      color: #18191c;
                      font-size: 16px;
                      font-weight: 600;
                      line-height: 24px;
                      overflow: hidden;
                      text-overflow: ellipsis;
                      white-space: nowrap;
                    "
                  >
                    {{ data.videoTitle || "Video Unavailable" }}
                  </div>

                  <div
                    v-if="data.fileTitle"
                    class="file-title"
                    style="
                      margin-top: 8px;
                      display: flex;
                      align-items: center;
                      color: #666;
                      font-size: 13px;
                    "
                  >
                    <span
                      class="file-label"
                      style="
                        flex-shrink: 0;
                        margin-right: 8px;
                        padding: 2px 7px;
                        border-radius: 4px;
                        background: #eef5ff;
                        color: #409eff;
                        font-size: 12px;
                        line-height: 18px;
                      "
                    >
                      Current Episode
                    </span>

                    <span
                      class="file-name"
                      style="
                        min-width: 0;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                      "
                    >
                      {{ data.fileTitle }}
                    </span>
                  </div>

                  <div
                    class="play-progress"
                    style="margin-top: 10px; color: #999; font-size: 13px"
                  >
                    <span
                      v-if="data.finished === 1"
                      class="finished"
                      style="color: #67c23a; font-weight: 500"
                    >
                      Watched
                    </span>

                    <span v-else>
                      Watched to {{ formatDuration(data.progress) }}
                    </span>

                    <span v-if="data.duration > 0">
                      / {{ formatDuration(data.duration) }}
                    </span>
                  </div>
                </div>

                <div
                  class="op-btns"
                  style="
                    margin-left: auto;
                    width: 44px;
                    flex: 0 0 44px;
                    display: flex;
                    align-items: center;
                    justify-content: flex-end;
                  "
                >
                  <div
                    class="iconfont icon-delete"
                    title="Delete Record"
                    @click="delHisotry(data.videoId)"
                    @mouseenter="
                      (e) => {
                        e.currentTarget.style.color = '#fff';
                        e.currentTarget.style.background = '#e3936c';
                      }
                    "
                    @mouseleave="
                      (e) => {
                        e.currentTarget.style.color = '#c0c4cc';
                        e.currentTarget.style.background = 'transparent';
                      }
                    "
                    style="
                      width: 30px;
                      height: 30px;
                      border-radius: 50%;
                      cursor: pointer;
                      color: #c0c4cc;
                      font-size: 18px;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      transition: all 0.15s ease;
                    "
                  ></div>
                </div>
              </div>
            </el-timeline-item>
          </template>
        </DataLoadMoreList>
      </div>
    </el-timeline>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useNavAction } from "@/stores/navActionStore";

const { proxy } = getCurrentInstance();
const router = useRouter();
const navActionStore = useNavAction();

const loadingData = ref(false);
const dataSource = ref({
  list: [],
  pageNo: 1,
  hasMore: true,
});

const loadDataList = async () => {
  if (loadingData.value || dataSource.value.hasMore === false) {
    return;
  }

  const params = {
    pageNo: dataSource.value.pageNo,
  };

  loadingData.value = true;
  const result = await proxy.Request({
    url: proxy.Api.playHistory,
    params,
  });
  loadingData.value = false;

  if (!result) {
    return;
  }

  const oldList = dataSource.value.list || [];
  dataSource.value = Object.assign({}, result.data);

  if (result.data.pageNo > 1) {
    dataSource.value.list = oldList.concat(result.data.list || []);
  } else {
    dataSource.value.list = result.data.list || [];
  }
};

loadDataList();

onMounted(() => {
  navActionStore.setShowHeader(true);
  navActionStore.setFixedHeader(true);
  navActionStore.setFixedCategory(false);
  navActionStore.setShowCategory(false);
});

const cleanAll = () => {
  proxy.Confirm({
    message: "Are you sure you want to clear the watch history?",
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.cleanHistory,
      });
      if (!result) {
        return;
      }

      proxy.Message.success("Deleted successfully");
      dataSource.value = {
        list: [],
        pageNo: 1,
        hasMore: false,
      };
    },
  });
};

const delHisotry = (videoId) => {
  proxy.Confirm({
    message: "Are you sure you want to delete this record?",
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.delHistory,
        params: {
          videoId,
        },
      });
      if (!result) {
        return;
      }

      proxy.Message.success("删除成功");
      dataSource.value.list = dataSource.value.list.filter((item) => {
        return item.videoId !== videoId;
      });
    },
  });
};

const goDetail = (videoId) => {
  if (!videoId) {
    return;
  }
  router.push(`/video/${videoId}`);
};

const formatDuration = (seconds) => {
  seconds = Number(seconds || 0);
  if (seconds <= 0) {
    return "00:00";
  }

  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);

  const mm = String(m).padStart(2, "0");
  const ss = String(s).padStart(2, "0");

  if (h > 0) {
    return `${h}:${mm}:${ss}`;
  }

  return `${mm}:${ss}`;
};

const getProgressPercent = (data) => {
  const progress = Number(data.progress || 0);
  const duration = Number(data.duration || 0);

  if (duration <= 0) {
    return 0;
  }

  return Math.min(100, Math.max(0, Math.floor((progress / duration) * 100)));
};
</script>

<style lang="scss" scoped>
.history-list {
  margin: 20px auto 0px;
  width: 1200px;

  .top-info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-left: 40px;
    margin-bottom: 20px;

    .icon-wating {
      font-size: 16px;

      &::before {
        margin-right: 5px;
        font-size: 22px;
        color: #e3936c;
        float: left;
      }
    }
  }

  .data-list {
    width: 100%;

    :deep(.el-timeline-item__content) {
      width: 100%;
    }

    .history-item {
      width: 100%;
      min-height: 132px;
      margin-top: 10px;
      padding: 12px 18px 12px 12px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-sizing: border-box;
      border-radius: 8px;
      background: #fff;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      .history-cover {
        width: 200px !important;
        height: 112px !important;
        flex: 0 0 200px !important;
        position: relative;
        overflow: hidden;
        border-radius: 6px;
        background: #f2f3f5;
        cursor: pointer;

        img {
          width: 200px !important;
          height: 112px !important;
          object-fit: cover !important;
          display: block !important;
        }

        .progress-bar {
          position: absolute;
          left: 0;
          right: 0;
          bottom: 0;
          height: 4px;
          background: rgba(0, 0, 0, 0.25);

          .progress-inner {
            height: 100%;
            background: var(--blue3);
          }
        }
      }

      .video-info {
        flex: 1;
        min-width: 0;
        align-self: stretch;
        display: flex;
        flex-direction: column;
        justify-content: center;

        .video-title {
          cursor: pointer;
          color: var(--text);
          font-size: 16px;
          font-weight: 600;
          line-height: 24px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;

          &:hover {
            color: var(--blue3);
          }
        }

        .file-title {
          margin-top: 8px;
          display: flex;
          align-items: center;
          color: var(--text2);
          font-size: 13px;

          .file-label {
            flex-shrink: 0;
            margin-right: 8px;
            padding: 2px 7px;
            border-radius: 4px;
            background: #eef5ff;
            color: var(--blue3);
            font-size: 12px;
            line-height: 18px;
          }

          .file-name {
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .play-progress {
          margin-top: 10px;
          color: var(--text3);
          font-size: 13px;

          .finished {
            color: #67c23a;
            font-weight: 500;
          }
        }
      }

      .op-btns {
        width: 44px;
        flex: 0 0 44px;
        display: flex;
        align-items: center;
        justify-content: flex-end;

        .iconfont {
          width: 30px;
          height: 30px;
          border-radius: 50%;
          cursor: pointer;
          color: #c0c4cc;
          font-size: 18px;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.15s ease;

          &:hover {
            color: #fff;
            background: #e3936c;
          }
        }
      }

      .op-btns {
        width: 44px;
        flex: 0 0 44px;
        display: flex;
        align-items: center;
        justify-content: flex-end;

        .iconfont {
          cursor: pointer;
          color: #c0c4cc;
          font-size: 18px;

          &:hover {
            color: #e3936c;
          }
        }
      }
    }
  }
}
</style>

