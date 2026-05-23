<template>
  <div class="video-list-panel">
    <div class="video-title">
      <router-link :to="`/user/${route.params.userId}/series`" class="a-link"
        >Playlists</router-link
      >
      &gt;
      {{ seriesInfo.seriesName }}
    </div>
    <div class="detail-panel">
      <div class="video-detail">
        <div class="count-info">
          <div>{{ videoList.length }} videos</div>
          <el-divider direction="vertical" />
          <div>Updated {{ proxy.Utils.formatDate(seriesInfo.updatedAt) }}</div>
        </div>
        <div class="description">
          {{ seriesInfo.seriesDescription }}
        </div>
      </div>
      <template v-if="myself">
        <el-button type="primary" @click="editSeries">Edit</el-button>
        <el-button type="danger" @click="delSeries">Delete</el-button>
      </template>
    </div>

    <VueDraggable
      v-model="videoList"
      @Update="reorder"
      handle=".move-handler"
      class="video-list"
      draggable=".list-item"
    >
      <template v-for="(item, index) in videoList" :key="item.seriesId">
        <div
          class="video-item-add"
          @click="addVideo"
          v-if="item.seriesId == 'add'"
        >
          <div class="iconfont icon-add"></div>
          <div class="add-info">Add Video</div>
        </div>
        <div class="list-item" v-else>
          <div class="cover" @click="jump(item)">
            <div class="move-handler iconfont icon-move" v-if="myself"></div>
            <Cover :source="item.videoCover" show404WhenEmpty />
          </div>
          <div class="list-name" @click="jump(item)">
            {{ item.videoTitle || "Video Unavailable" }}
          </div>
          <div class="play-count-info">
            <div class="play-count iconfont icon-play2">
              {{ item.playCount }}
            </div>
            <div class="create-time">
              {{ proxy.Utils.formatDate(item.createdAt) }}
            </div>
            <el-dropdown>
              <div class="iconfont icon-more" @click.stop v-show="myself"></div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click.stop="delVideo(item)"
                    >Delete</el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>
    </VueDraggable>
    <NoData v-if="videoList.length == 0" msg="No videos yet"></NoData>

    <VideoSeriesEdit
      ref="videoSeriesEditRef"
      @reload="getSeriesDetail"
    ></VideoSeriesEdit>
  </div>
</template>

<script setup>
import VideoSeriesEdit from "./VideoSeriesEdit.vue";
import { VueDraggable } from "vue-draggable-plus";
import { ref, reactive, getCurrentInstance, nextTick, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
import { useLoginStore } from "@/stores/loginStore.js";
const loginStore = useLoginStore();

//是否是自己
const myself = computed(() => {
  return (
    loginStore.isLogin &&
    String(loginStore.loginUserId) === String(route.params.userId)
  );
});

const checkManage = () => {
  if (!loginStore.isLogin) {
    loginStore.setLogin(true);
    return false;
  }

  if (!myself.value) {
    proxy.Message.warning("You can't manage another user's playlists");
    return false;
  }

  return true;
};
const seriesInfo = ref({});
const rawVideoList = ref([]);

const videoList = computed({
  get() {
    if (myself.value) {
      return [
        {
          seriesId: "add",
        },
        ...rawVideoList.value,
      ];
    }

    return rawVideoList.value;
  },

  set(list) {
    rawVideoList.value = list.filter((item) => {
      return item.seriesId !== "add";
    });
  },
});

const getSeriesDetail = async () => {
  let result = await proxy.Request({
    url: proxy.Api.uHomeSeriesGetVideoSeriesDetail,
    params: {
      seriesId: route.params.seriesId,
    },
  });

  if (!result) {
    return;
  }

  seriesInfo.value = result.data.videoSeries;
  rawVideoList.value = result.data.seriesVideoList || [];
};
getSeriesDetail();

const reorder = async () => {
  if (!checkManage()) {
    getSeriesDetail();
    return;
  }

  let videoIds = rawVideoList.value
    .filter((item) => item.videoId)
    .map((item) => item.videoId);

  let result = await proxy.Request({
    url: proxy.Api.uHomeSeriesReorderSeriesVideo,
    params: {
      seriesId: route.params.seriesId,
      videoIds: videoIds.join(","),
    },
  });

  if (!result) {
    getSeriesDetail();
    return;
  }

  proxy.Message.success("Video order updated successfully");
};

const jump = (item) => {
  router.push(`/video/${item.videoId}`);
};

const delVideo = (item) => {
  if (!checkManage()) {
    return;
  }

  proxy.Confirm({
    message: `Are you sure you want to delete "${item.videoTitle}"?`,
    okfun: async () => {
      if (!checkManage()) {
        return;
      }

      let result = await proxy.Request({
        url: proxy.Api.uHomeSeriesDelSeriesVideo,
        params: {
          seriesId: route.params.seriesId,
          videoId: item.videoId,
        },
      });

      if (!result) {
        return;
      }

      proxy.Message.success("Deleted successfully");
      getSeriesDetail();
    },
  });
};

const delSeries = () => {
  if (!checkManage()) {
    return;
  }

  proxy.Confirm({
    message: `Are you sure you want to delete "${seriesInfo.value.seriesName}"?`,
    okfun: async () => {
      if (!checkManage()) {
        return;
      }

      let result = await proxy.Request({
        url: proxy.Api.uHomeSeriesDelVideoSeries,
        params: {
          seriesId: route.params.seriesId,
        },
      });

      if (!result) {
        return;
      }

      proxy.Message.success("Deleted successfully");
      router.push(`/user/${route.params.userId}/series`);
    },
  });
};
//编辑系列
const videoSeriesEditRef = ref();
const editSeries = () => {
  if (!checkManage()) {
    return;
  }

  videoSeriesEditRef.value.show(seriesInfo.value, 1);
};

const addVideo = () => {
  if (!checkManage()) {
    return;
  }

  videoSeriesEditRef.value.show(seriesInfo.value, 2);
};
</script>

<style lang="scss" scoped>
.video-list-panel {
  padding: 20px;
  border-radius: 5px;
  background: #fff;
  .video-title {
    font-size: 16px;
  }
  .video-list {
    margin-top: 20px;
    display: grid;
    grid-gap: 20px;
    grid-template-columns: repeat(6, 1fr);
    .video-item-add {
      border-radius: 5px;
      width: 100%;
      height: 150px;
      border: 2px dashed #ddd;
      text-align: center;
      color: var(--text3);
      cursor: pointer;
      .icon-add {
        font-size: 40px;
        padding-top: 40px;
      }
    }
    .list-item {
      .cover {
        position: relative;
        .move-handler {
          width: 100%;
          height: 30px;
          cursor: move;
          position: absolute;
          left: 0px;
          top: 0px;
          background: #fff;
          z-index: 100;
          border-radius: 5px 5px 0px 0px;
          border: 1px solid #ddd;
          display: flex;
          align-items: center;
          justify-content: center;
          display: none;
        }
        &:hover {
          .move-handler {
            display: flex;
          }
        }
      }
      .list-name {
        cursor: pointer;
        font-size: 13px;
        margin-top: 5px;
        height: 35px;
        color: var(--text2);
        margin-top: 10px;
        display: -webkit-box;
        overflow: hidden;
        -webkit-box-orient: vertical;
        text-overflow: -o-ellipsis-lastline;
        text-overflow: ellipsis;
        word-break: break-word !important;
        word-break: break-all;
        line-break: anywhere;
        -webkit-line-clamp: 2;
      }
      .play-count-info {
        margin-top: 5px;
        display: flex;
        color: var(--text3);
        align-items: center;
        font-size: 12px;
        .play-count {
          flex: 1;
          &::before {
            margin-right: 5px;
          }
        }
        .icon-more {
          margin-left: 5px;
          cursor: pointer;
        }
      }
    }
  }
}

.detail-panel {
  padding: 20px 0px;
  font-size: 13px;
  color: var(--text3);
  border-bottom: 1px solid #ddd;
  display: flex;
  .video-detail {
    flex: 1;
    .count-info {
      display: flex;
      align-items: center;
    }
    .description {
      margin-top: 10px;
    }
  }
}
</style>
