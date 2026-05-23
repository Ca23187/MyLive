<template>
  <div class="my-video-list-title" v-if="seriesList.length > 0">
    <router-link class="title" :to="`/user/${route.params.userId}/series`"
      >Playlists</router-link
    >
    <span
      class="iconfont icon-add op-btn new-btn"
      @click="showVieoSeries"
      v-if="myself"
      >New</span
    >
  </div>
  <div class="part-item" v-for="item in seriesList">
    <div class="part-title">
      <div class="title-panel">
        <router-link
          class="title"
          :to="`/user/${route.params.userId}/series/${item.seriesId}`"
          >{{ item.seriesName }}
        </router-link>
        <div class="count-info">{{ item.videoInfoList.length }}</div>
      </div>
      <router-link
        class="op-btn"
        :to="`/user/${route.params.userId}/series/${item.seriesId}`"
        >More &gt;</router-link
      >
    </div>
    <div class="video-list5">
      <VideoItem :data="video" v-for="video in item.videoInfoList"> </VideoItem>
    </div>
  </div>
  <VideoSeriesEdit
    ref="videoSeriesEditRef"
    @reload="loadSeriesList"
  ></VideoSeriesEdit>
</template>

<script setup>
import "@/assets/scss/uhome.scss";
import VideoSeriesEdit from "./VideoSeriesEdit.vue";
import VideoItem from "./VideoItem.vue";
import { ref, reactive, getCurrentInstance, nextTick, computed } from "vue";
const { proxy } = getCurrentInstance();
import { useRoute, useRouter } from "vue-router";
const route = useRoute();
const router = useRouter();
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
//视频系列
const seriesList = ref([]);
const loadSeriesList = async () => {
  let result = await proxy.Request({
    url: proxy.Api.uHomeSeriesLoadVideoSeriesWithVideo,
    params: {
      userId: route.params.userId,
    },
  });
  if (!result) {
    return;
  }
  seriesList.value = result.data;
};
loadSeriesList();

const videoSeriesEditRef = ref();
const showVieoSeries = () => {
  if (!checkManage()) {
    return;
  }

  videoSeriesEditRef.value.show();
};
</script>

<style lang="scss" scoped>
.my-video-list-title {
  margin-top: 15px;
  .title {
    text-decoration: none;
    color: var(--text);
    font-size: 16px;
  }
  .new-btn {
    margin-left: 10px;
    padding: 5px 10px;
    cursor: pointer;
  }
}

.part-title {
  .title-panel {
    .title {
      font-size: 15px;
    }
  }
}
</style>
