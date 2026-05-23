<template>
  <div class="player-panel">
    <div
      ref="playerRef"
      class="player-style"
      :style="{ height: playerHeight + 'px' }"
    ></div>
    <div class="danmu-panel">
      <div class="watcher">
        {{ onLineCount }} watching , {{ danmakuCount }} danmaku loaded
      </div>
      <div id="danmu" v-show="showDanmu"></div>
      <div v-show="!showDanmu" class="close-danmu">Danmaku disabled</div>
    </div>
    <div id="play"><img :src="proxy.Utils.getLocalImage('play.png')" /></div>
  </div>
</template>
<script setup>
import { mitter } from "@/eventbus/eventBus.js";
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUnmounted,
  onBeforeUnmount,
  watch,
  inject,
  computed,
} from "vue";
import Hls from "hls.js";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
import Artplayer from "artplayer";
import artplayerPluginDanmuku from "artplayer-plugin-danmuku";
//https://artplayer.org/

import { useLoginStore } from "@/stores/loginStore.js";
const loginStore = useLoginStore();

const props = defineProps({
  fileId: {
    type: String,
    default: "",
  },
});

const playerRef = ref();
const options = {
  url: proxy.Api.getVideoResource,
};

let player = null;
let currentTime = null;
let startTime = null;

const initPlayer = () => {
  //隐藏右键菜单
  Artplayer.CONTEXTMENU = false;
  //自动回放功能的最大记录数，默认为 10
  Artplayer.AUTO_PLAYBACK_MAX = 20;
  //自动回放功能的最小记录时长，单位为秒，默认为 5
  Artplayer.AUTO_PLAYBACK_MIN = 10;
  player = new Artplayer({
    container: playerRef.value,
    lang: "en",
    url: ``,
    type: "m3u8",
    customType: {
      m3u8: function (video, url, art) {
        if (Hls.isSupported()) {
          if (art.hls) art.hls.destroy();
          const hls = new Hls();
          hls.loadSource(url);
          hls.attachMedia(video);
          art.hls = hls;
          art.on("destroy", () => hls.destroy());
        } else if (video.canPlayType("application/vnd.apple.mpegurl")) {
          video.src = url;
        } else {
          art.notice.show = "Your browser does not support this player";
        }
      },
    },
    theme: "#23ade5", //播放器主题颜色，目前用于 进度条 和 高亮元素 上
    volume: 0.7, //播放器的默认音量
    autoplay: true, //是否自动播放 假如希望默认进入页面就能自动播放视频，muted 必需为 true
    autoMini: false, //当播放器滚动到浏览器视口以外时，自动进入 迷你播放 模式
    fullscreen: true, //设置和获取播放器窗口全屏
    fullscreenWeb: true, //设置和获取播放器网页全屏
    setting: true,
    pip: true, //开启画中画
    playbackRate: true, //是否显示视频播放速度功能，会出现在 设置面板 和 右键菜单 里
    flip: true, //是否显示视频翻转功能，目前只出现在 设置面板 和 右键菜单
    aspectRatio: true, //比例
    //miniProgressBar: true, //迷你进度条，只在播放器失去焦点后且正在播放时出现
    screenshot: true, //截图
    autoPlayback: true, //自动回放
    //自定义图标
    icons: {
      state: document.querySelector("#play"),
    },
    controls: [
      {
        name: "wide-screen",
        position: "right",
        html: '<span class="iconfont icon-wide-screen"></span>',
        tooltip: "Wide Screen",
        style: {
          color: "#fff",
          display: "flex",
        },
        click: function (...args) {
          changeWideScreen();
        },
      },
      {
        name: "narrow-screen",
        position: "right",
        html: '<span class="iconfont icon-narrow-screen"></span>',
        tooltip: "Exit Wide Screen",
        style: {
          color: "#fff",
          display: "none",
        },
        click: function (...args) {
          changeWideScreen();
        },
      },
    ],
    plugins: [
      artplayerPluginDanmuku({
        mount: document.querySelector("#danmu"),
        theme: "light",
        emitter: true,
        danmuku: function () {
          return new Promise(async (resovle) => {
            //是否展示弹幕
            const danmuList = await loadDanmakuList();
            return resovle(danmuList);
          });
        },
        beforeEmit: async (danmu) => {
          if (videoInfo.value?.allowDanmaku === 0) {
            proxy.Message.warning("Danmaku is disabled for this video");
            return false;
          }

          let result = await postDanmaku(danmu);
          //重新获取一下弹幕列表
          loadDanmakuList();
          if (!result) {
            return false;
          }
          mitter.emit("danmSend");
          proxy.Message.success("Danmaku sent successfully");
          return true;
        },
      }),
    ],
  });
  player.on("play", () => {
    startReportTimer();
  });

  player.on("pause", () => {
    reportPlayProgress();
    stopReportTimer();
  });

  player.on("hover", (state) => {
    let display = "none";
    if (state) {
      display = "flex";
    }
    player.template.$bottom.style.display = display;
  });
  //视频播放完成
  player.on("video:ended", () => {
    reportPlayProgress(true);
    stopReportTimer();
    mitter.emit("playEnd");
  });
};

const emit = defineEmits(["changeWideScreen"]);
const wideScreen = ref(false);
const changeWideScreen = () => {
  wideScreen.value = !wideScreen.value;
  if (wideScreen.value) {
    player.controls["wide-screen"].style.display = "none";
    player.controls["narrow-screen"].style.display = "flex";
  } else {
    player.controls["wide-screen"].style.display = "flex";
    player.controls["narrow-screen"].style.display = "none";
  }
  emit("changeWideScreen", wideScreen.value);
};

const fileId = ref();
const postDanmaku = (danmu) => {
  if (!loginStore.isLogin) {
    loginStore.setLogin(true);
    return;
  }
  danmu.fileId = fileId.value;
  danmu.videoId = route.params.videoId;
  danmu.videoUserId = videoInfo.value?.userId;
  danmu.time = Math.round(danmu.time);
  return proxy.Request({
    url: proxy.Api.postDanmaku,
    params: danmu,
  });
};

//弹幕数量
const danmakuCount = ref(0);
const loadDanmakuList = async () => {
  if (!fileId.value) {
    return [];
  }
  let result = await proxy.Request({
    url: proxy.Api.loadDanmaku,
    params: { fileId: fileId.value, videoId: route.params.videoId },
  });
  if (!result) {
    return [];
  }
  mitter.emit("loadDanmaku", result.data);
  danmakuCount.value = result.data.length;
  return result.data;
};

const playerHeight = ref(500);
const setPlayerHeight = inject("playerHeight");

const handleVisibilityChange = () => {
  if (document.hidden) {
    reportPlayProgress();
    stopReportTimer();
  } else {
    if (player?.playing) {
      startReportTimer();
    }
  }
};

onMounted(() => {
  nextTick(() => {
    initPlayer();

    if (props.fileId) {
      changeFile(props.fileId);
    }

    const height = Math.round((playerRef.value.clientWidth - 8) * 0.5625);
    playerHeight.value = height;
    setPlayerHeight(height);
  });

  mitter.on("changeP", (_fileId) => {
    changeFile(_fileId);
  });
  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onBeforeUnmount(() => {
  reportPlayProgress();

  stopReportTimer();

  if (player) {
    player.destroy(false);
  }

  mitter.off("changeP");
  document.removeEventListener("visibilitychange", handleVisibilityChange);
});

//获取在线人数轮训上报，类似上报心跳
// 在线人数心跳 timer
let onlineTimer = null;

// 播放进度上报 timer
let progressTimer = null;

const REPORT_INTERVAL = 10000;

const startReportTimer = () => {
  startOnlineTimer();
  startProgressTimer();
};

const stopReportTimer = () => {
  stopOnlineTimer();
  stopProgressTimer();
};

const startOnlineTimer = () => {
  stopOnlineTimer();

  reportVideoPlayOnline();

  onlineTimer = setInterval(() => {
    reportVideoPlayOnline();
  }, REPORT_INTERVAL);
};

const stopOnlineTimer = () => {
  if (onlineTimer !== null) {
    clearInterval(onlineTimer);
    onlineTimer = null;
  }
};

const startProgressTimer = () => {
  stopProgressTimer();

  reportPlayProgress();

  progressTimer = setInterval(() => {
    reportPlayProgress();
  }, REPORT_INTERVAL);
};

const stopProgressTimer = () => {
  if (progressTimer !== null) {
    clearInterval(progressTimer);
    progressTimer = null;
  }
};

const onLineCount = ref(1);
const reportVideoPlayOnline = async () => {
  if (!fileId.value) {
    return;
  }
  let result = await proxy.Request({
    url: proxy.Api.reportVideoPlayOnline,
    params: {
      fileId: fileId.value,
      deviceId: loginStore.deviceId,
    },
    showError: false,
  });
  if (!result) {
    return;
  }
  onLineCount.value = result.data;
};

let lastReportProgress = -1;
let lastReportFileId = null;

const reportPlayProgress = async (finished = false, force = false) => {
  if (!fileId.value || !player) {
    return;
  }

  if (!loginStore.isLogin) {
    return;
  }

  const progress = Math.floor(player.currentTime || 0);
  const duration = Math.floor(player.duration || 0);

  if (!force && progress <= 0 && duration <= 0 && !finished) {
    return;
  }

  const isFinished = finished || (duration > 0 && progress >= duration - 5);

  if (
    !force &&
    !isFinished &&
    lastReportFileId === fileId.value &&
    lastReportProgress === progress
  ) {
    return;
  }

  lastReportFileId = fileId.value;
  lastReportProgress = progress;

  await proxy.Request({
    url: proxy.Api.reportPlayProgress,
    params: {
      videoId: route.params.videoId,
      fileId: fileId.value,
      progress,
      finished: isFinished ? 1 : 0,
    },
    showError: false,
  });
};

//判断是否显示弹幕
const videoInfo = inject("videoInfo");
const showDanmu = computed(() => {
  return videoInfo.value?.allowDanmaku !== 0;
});

watch(
  () => videoInfo.value?.allowDanmaku,
  (val) => {
    if (!player) return;

    if (val === 0) {
      player.plugins.artplayerPluginDanmuku.config.emitter = false;
    } else {
      player.plugins.artplayerPluginDanmuku.config.emitter = true;
    }
  },
  { immediate: true }
);

const changeFile = (_fileId) => {
  if (!_fileId || _fileId === fileId.value) {
    return;
  }

  if (fileId.value) {
    reportPlayProgress();
    stopReportTimer();
  }

  fileId.value = _fileId;

  if (player) {
    player.switch = `${proxy.Api.getVideoResource}/${_fileId}/`;
    player.plugins.artplayerPluginDanmuku.load();
  }

  reportPlayProgress(false, true);
  reportVideoPlayOnline();

  if (player?.playing) {
    startReportTimer();
  }
};

watch(
  () => props.fileId,
  (newFileId) => {
    changeFile(newFileId);
  }
);
</script>

<style>
</style>

<style lang="scss" scoped>
.player-panel {
  .player-style {
    width: 100%;
    :deep(.art-video-player.art-mask-show .art-state) {
      //播放按钮
      position: absolute;
      right: 40px;
      bottom: 50px;
      .art-icon-state {
        width: 60px;
        height: 60px;
        img {
          width: 100%;
        }
      }
    }
    //改变播放器的右侧操作按钮
    :deep(.art-controls-right) {
      position: relative;
      display: block;
      width: 280px;
      .art-control {
        position: absolute;
      }
      //截屏
      .art-control-screenshot {
        left: 0px;
      }
      //设置按钮
      .art-control-setting {
        left: 46px;
      }
      //画中画
      .art-control-pip {
        left: 92px;
      }
      //宽屏
      .art-control-wide-screen,
      .art-control-narrow-screen {
        left: 138px;
        .iconfont {
          font-size: 20px;
        }
      }
      //网页全屏按钮
      .art-control-fullscreenWeb {
        left: 184px;
      }
      //全屏按钮
      .art-control-fullscreen {
        left: 230px;
      }
    }
  }
  .danmu-panel {
    box-shadow: 0 2px 6px #ddd;
    height: 56px;
    border-top: none;
    background: #fff;
    display: flex;
    align-items: center;
    padding: 0px 15px;
    .watcher {
      width: 250px;
      color: var(--text2);
    }
    #danmu {
      flex: 1;
    }
    .close-danmu {
      flex: 1;
      color: var(--text2);
      text-align: center;
    }
  }

  :deep(.danmu-setting) {
    background: none !important;
    padding: 0px;
    border: none;
    .inner-panel {
      width: 300px;
      height: 280px;
      position: relative;
      .inner {
        position: absolute;
        left: 0px;
        top: 0px;
        z-index: 99999;
        color: #fff;
      }
      .mask {
        position: absolute;
        top: 0px;
        right: 0px;
        left: 0px;
        bottom: 0px;
        background: #000;
        opacity: 0.6;
      }
    }
  }
}
</style>
