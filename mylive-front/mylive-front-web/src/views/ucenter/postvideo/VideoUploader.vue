<template>
  <div v-show="!startUpload" class="uploader-start-panel">
    <VideoUploadStart @addFile="addFile"></VideoUploadStart>
  </div>
  <div v-if="startUpload">
    <div
      class="file-list"
      v-draggable="[fileList, { animation: 150, handle: '.video-p' }]"
    >
      <div class="file-item" v-for="(item, index) in fileList" :key="index">
        <div class="video-p">
          <div class="iconfont icon-video"></div>
          <div class="video-p-info">P{{ index + 1 }}</div>
        </div>
        <div class="video-info">
          <div class="video-title">
            <div class="upload-info">
              <div class="title">
                <el-input
                  v-show="item.edit"
                  :id="'file-input' + item.uid"
                  v-model="item.title"
                  size="small"
                  @blur="endEdit(index)"
                ></el-input>
                <div
                  v-show="!item.edit"
                  class="title-show"
                  @click="editTitle(index)"
                >
                  {{ item.title }}
                </div>
              </div>
              <div class="upload-status">
                <span v-if="item.status == 'uploading'">
                  Uploaded: {{ proxy.Utils.size2Str(item.uploadSize) }}/{{
                    proxy.Utils.size2Str(item.totalSize)
                  }}
                </span>
                <span
                  v-else
                  :class="['iconfont', 'icon-' + STATUS[item.status].icon]"
                  :style="{ color: STATUS[item.status].color }"
                >
                  {{ STATUS[item.status].desc }}</span
                >
              </div>
            </div>
            <div class="op">
              <div class="item percent" v-if="item.status == 'uploading'">
                {{ item.uploadPercent }}%
              </div>
              <template v-if="item.status == 'uploading'">
                <div
                  v-if="item.pause"
                  class="item iconfont icon-play3"
                  @click="resumeUpload(item.uid)"
                ></div>
                <div
                  v-else
                  class="item iconfont icon-pause"
                  @click="pauseUpload(item.uid)"
                ></div>
              </template>
              <div class="item iconfont icon-del" @click="delFile(index)"></div>
            </div>
          </div>
          <div
            class="video-progress"
            v-if="item.status == 'uploading' || item.status == 'success'"
          >
            <el-progress
              :percentage="item.uploadPercent"
              :show-text="false"
              :stroke-width="3"
              :status="item.status == 'uploading' ? '' : 'success'"
            >
            </el-progress>
          </div>
        </div>
      </div>
    </div>
    <div
      class="add-video-btn"
      v-if="fileList.length < sysSettingStore.sysSetting.videoPartCount"
    >
      <el-upload
        multiple
        :show-file-list="false"
        :http-request="addFile"
        :accept="proxy.videoAccept"
      >
        <el-button type="primary">Add Videos</el-button>
      </el-upload>
    </div>
  </div>
</template>

<script setup>
import { useSysSettingStore } from "@/stores/sysSettingStore.js";
const sysSettingStore = useSysSettingStore();

import VideoUploadStart from "./VideoUploadStart.vue";
import { vDraggable } from "vue-draggable-plus";
import {
  computed,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUnmounted,
  ref,
} from "vue";
import { useRouter } from "vue-router";

const { proxy } = getCurrentInstance();
const router = useRouter();

import { mitter } from "@/eventbus/eventBus.js";

const props = defineProps({
  videoList: {
    type: Array,
    default: [],
  },
});

const STATUS = {
  emptyfile: {
    value: "emptyfile",
    desc: "File is empty",
    color: "#F75000",
    icon: "error",
  },
  largefile: {
    value: "largefile",
    desc: "File exceeds size limit: " + sysSettingStore.sysSetting.videoSize + "MB",
    color: "#F75000",
    icon: "error",
  },
  wating: {
    value: "wating",
    desc: "Waiting",
    color: "#e6a23c",
    icon: "wating",
  },
  uploading: {
    value: "uploading",
    desc: "Uploading",
    color: "#409eff",
    icon: "upload",
  },
  fail: {
    value: "fail",
    desc: "Upload Failed",
    color: "#F75000",
    icon: "error",
  },
  success: {
    value: "success",
    desc: "Upload Complete",
    color: "#67c23a",
    icon: "success",
  },
};

//分片大小
const CHUNK_SIZE = proxy.chunkSize;
const MAX_CHUNK_UPLOADING = proxy.maxChunkUploading;

//同时最大上传数量
const MAX_UPLOADING = proxy.maxUploading;

const fileList = ref([]);

const getFileByUid = (uid) => {
  const currentFile = fileList.value.find((item) => {
    return item.uid == uid;
  });
  return currentFile;
};

const startUpload = ref(false);
mitter.on("startUpload", () => {
  fileList.value = [];
  startUpload.value = true;
});

const checkVideoFile = (file) => {
  const allowExts = ['mp4', 'webm', 'mov', 'avi', 'mkv', 'rmvb']

  const fileExt = file.name.includes('.')
    ? file.name.split('.').pop().toLowerCase()
    : ''

  const validType = file.type && file.type.startsWith('video/')
  const validExt = allowExts.includes(fileExt)

  if (!validType && !validExt) {
    proxy.Message.warning('Only video files can be uploaded')
    return false
  }

  return true
}

const addFile = (file) => {
  file = file.file || file

  if (!checkVideoFile(file)) {
    return
  }

  if (fileList.value.length >= sysSettingStore.sysSetting.videoPartCount) {
    proxy.Message.warning(
      "You can add up to " + sysSettingStore.sysSetting.videoPartCount + " videos"
    );
    return;
  }

  let title = file.name;
  const lastPoint = title.lastIndexOf(".");
  title = lastPoint == -1 ? title : title.substring(0, lastPoint);
  const fileItem = {
    file: file,
    //文件UID
    uid: file.uid,
    //文件名
    title: title,
    //上传状态
    status: STATUS.wating.value,
    //已上传大小
    uploadSize: 0,
    //文件总大小
    totalSize: file.size,
    //上传进度
    uploadPercent: 0,
    //暂停
    pause: false,

    uploadedChunks: new Set(),
    chunkProgressMap: {},
    
    //当前分片
    chunkIndex: 0,
    //错误信息
    errorMsg: null,
  };
  //加入文件
  fileList.value.push(fileItem);
  if (fileItem.totalSize == 0) {
    fileItem.status = STATUS.emptyfile.value;
    return;
  }
  //判断文件大小
  if (fileItem.totalSize > sysSettingStore.sysSetting.videoSize * 1024 * 1024) {
    fileItem.status = STATUS.largefile.value;
    return;
  }

  let uploadingFiles = fileList.value.filter((item) => {
    return item.status == STATUS.uploading.value;
  });

  if (uploadingFiles.length >= MAX_UPLOADING) {
    return;
  }
  uploadFile(fileItem.uid);
};

const uploadFile = async (uid) => {
  const currentFile = getFileByUid(uid);
  if (!currentFile) return;

  currentFile.status = STATUS.uploading.value;
  currentFile.pause = false;

  const file = currentFile.file;
  const fileSize = currentFile.totalSize;
  const chunks = Math.ceil(fileSize / CHUNK_SIZE);

  if (!currentFile.uploadId) {
    const resultData = await proxy.Request({
      url: proxy.Api.preUploadVideo,
      params: {
        chunks,
        fileSize,
      },
      errorCallback: (errorMsg) => {
        currentFile.status = STATUS.fail.value;
        currentFile.errorMsg = errorMsg;
      },
    });

    if (!resultData) return;

    currentFile.uploadId = resultData.data;
  }

  const calcProgress = () => {
    let uploadSize = 0;

    for (const size of Object.values(currentFile.chunkProgressMap)) {
      uploadSize += size;
    }

    currentFile.uploadSize = Math.min(uploadSize, fileSize);
    currentFile.uploadPercent = Math.floor(
      (currentFile.uploadSize / fileSize) * 100
    );
  };

  const waitUploadChunks = [];

  for (let i = 0; i < chunks; i++) {
    if (!currentFile.uploadedChunks.has(i)) {
      waitUploadChunks.push(i);
    }
  }

  let cursor = 0;

  const uploadChunk = async (i) => {
    if (currentFile.pause || currentFile.del) return;

    const start = i * CHUNK_SIZE;
    const end = Math.min(start + CHUNK_SIZE, fileSize);
    const chunkFile = file.slice(start, end);

    const uploadResult = await proxy.Request({
      url: proxy.Api.uploadVideo,
      dataType: "file",
      params: {
        chunkFile,
        chunkIndex: i,
        uploadId: currentFile.uploadId,
      },
      showError: false,
      errorCallback: (errorMsg) => {
        currentFile.status = STATUS.fail.value;
        currentFile.errorMsg = errorMsg;
      },
      uploadProgressCallback: (event) => {
        currentFile.chunkProgressMap[i] = Math.min(
          event.loaded || 0,
          chunkFile.size
        );
        calcProgress();
      },
    });

    if (uploadResult == null) {
      throw new Error("chunk upload fail");
    }

    currentFile.uploadedChunks.add(i);
    currentFile.chunkProgressMap[i] = chunkFile.size;
    calcProgress();
  };

  const workers = new Array(MAX_CHUNK_UPLOADING).fill(null).map(async () => {
    while (cursor < waitUploadChunks.length) {
      if (currentFile.pause || currentFile.del) break;
      if (currentFile.status === STATUS.fail.value) break;

      const i = waitUploadChunks[cursor++];
      await uploadChunk(i);
    }
  });

  try {
    await Promise.all(workers);
  } catch (e) {
    currentFile.status = STATUS.fail.value;
    return;
  }

  if (currentFile.pause || currentFile.del) return;

  if (currentFile.uploadedChunks.size === chunks) {
    currentFile.status = STATUS.success.value;
    currentFile.uploadPercent = 100;
    currentFile.uploadSize = fileSize;

    const nextItem = fileList.value.find((item) => {
      return item.status == STATUS.wating.value;
    });

    if (nextItem) {
      uploadFile(nextItem.uid);
    }
  }
};

//暂停
const pauseUpload = (uid) => {
  const currentFile = getFileByUid(uid);
  currentFile.pause = true;
};
//继续上传
const resumeUpload = (uid) => {
  const currentFile = getFileByUid(uid);
  currentFile.pause = false;
  uploadFile(uid);
};
//删除文件
const delFile = async (index) => {
  const currentFile = fileList.value[index];
  currentFile.del = true;
  fileList.value.splice(index, 1);
  if (currentFile.fileId) {
    return;
  }
  //如果是新上传的文件，删除直接删除服务器上的临时文件
  await proxy.Request({
    url: proxy.Api.delUploadVideo,
    dataType: "file",
    params: {
      uploadId: currentFile.uploadId,
    },
    showError: false,
  });
};

//编辑标题
const editTitle = (index) => {
  const currentFile = fileList.value[index];
  currentFile.edit = true;
  nextTick(() => {
    const input = document.querySelector("#file-input" + currentFile.uid);
    setTimeout(() => {
      input.focus();
    }, 100);
  });
};

const endEdit = (index) => {
  const currentFile = fileList.value[index];
  currentFile.edit = false;
};

//获取文件列表
const getUploadFileList = () => {
  let failCount = 0;
  let noUploadCount = 0;

  for (
    let i = 0, item;
    (item = fileList.value[i]), i < fileList.value.length;
    i++
  ) {
    if (
      item.status === STATUS.fail.value ||
      item.status == STATUS.emptyfile.value
    ) {
      failCount++;
      continue;
    }

    if (
      item.status === STATUS.uploading.value ||
      item.status === STATUS.wating.value
    ) {
      noUploadCount++;
      continue;
    }
  }

  if (failCount > 0) {
    proxy.Message.warning("Please remove failed uploads");
    return null;
  }
  if (noUploadCount > 0) {
    proxy.Message.warning("Upload is not complete yet");
    return null;
  }
  const uploadFileList = fileList.value.map((item) => {
    return {
      uploadId: item.uploadId,
      fileId: item.fileId,
      title: item.title,
    };
  });
  return uploadFileList;
};

const initUploader = (_startUpload, videoList) => {
  startUpload.value = _startUpload;
  fileList.value.splice(0, fileList.value.length);
  videoList.forEach((item) => {
    if (item.transcodeResult == 1) {
      item.status = STATUS.success.value;
    } else {
      item.status = STATUS.fail.value;
    }
    item.uid = item.fileId;
    item.uploadPercent = 100;
    fileList.value.push(item);
  });
};

defineExpose({
  getUploadFileList,
  initUploader,
});

onUnmounted(() => {
  mitter.off("startUpload");
});
</script>

<style lang="scss" scoped>
.file-list {
  background: #f6f7f8;
  border-radius: 5px;
  margin: 0px 200px 0px 20px;
  .file-item {
    padding: 10px;
    display: flex;
    margin-bottom: 10px;
    .video-p {
      flex-shrink: 0;
      position: relative;
      width: 44px;
      height: 40px;
      cursor: move;

      .icon-video {
        font-size: 40px;
        color: #a6def1;
        padding: 0px;
      }

      .video-p-info {
        width: 35px;
        line-height: 40px;
        text-align: center;
        color: #fff;
        top: 0px;
        left: 0px;
        z-index: 1;
        position: absolute;
      }
    }

    .video-info {
      flex: 1;
      min-width: 0;
      padding-left: 10px;

      .video-title {
        display: flex;
        align-items: center;
        width: 100%;

        .upload-info {
          width: 100%;
          min-width: 0;
          flex: 1;

          .title {
            width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            .title-show {
              line-height: 24px;
              padding-left: 7px;
              font-size: 12px;
            }
          }

          .upload-status {
            margin-top: 5px;
            color: #999;
            font-size: 12px;
            .iconfont {
              font-size: 12px;

              &::before {
                font-size: 16px;
                margin-right: 2px;
              }
            }
          }
        }

        .op {
          margin-left: 10px;
          display: flex;
          align-items: center;
          color: #909090;

          .item {
            margin-right: 10px;
            font-size: 13px;
          }

          .percent {
            width: 30px;
          }

          .iconfont {
            cursor: pointer;
            font-size: 20px;
            color: #909090;
          }
        }
      }

      .video-progress {
        margin-top: 5px;
      }
    }
  }
}
.add-video-btn {
  padding-left: 20px;
  margin-bottom: 10px;
}
</style>
