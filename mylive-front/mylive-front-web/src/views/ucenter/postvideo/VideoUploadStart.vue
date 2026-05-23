<template>
  <div class="uploader-start-panel">
    <el-upload class="uploader-start" drag multiple :show-file-list="false" :http-request="addFile" :before-upload="startUpload"
      :accept="proxy.videoAccept">
      <div class="upload-handler">
        <div class="iconfont icon-upload"></div>
        <div class="info">Drag and drop files here to upload</div>
        <div class="upload-btn">Upload Video</div>
      </div>
    </el-upload>
  </div>

  <div class="upload-explain">
    <el-popover placement="top-end" :width="400" trigger="hover">
      <template #reference>
        <div class="item">Video Size</div>
      </template>
      <div>
        <p>The maximum file size for web uploads is {{sysSettingStore.sysSetting.videoSize}} MB</p>
        <p>Videos can be up to 3 hours long</p>
        <p>For long or large videos, split them into parts or submit them as a collection</p>
      </div>
    </el-popover>

    <el-popover placement="top-end" :width="420" trigger="hover">
      <template #reference>
        <div class="item">Video Format</div>
      </template>
      <div>
        <p>Recommended format: MP4</p>
        <p>Recommended formats transcode faster and may pass review more quickly</p>
        <p>Other supported formats: MP4, AVI, RMVB, MKV, MOV</p>
      </div>
    </el-popover>

    <el-popover placement="top-end" :width="350" trigger="hover">
      <template #reference>
        <div class="item">Video Bitrate</div>
      </template>
      <div>
        <p>Maximum supported resolution: 8192×4320</p>
        <p>Recommended resolution: 1920×1080 or 3840×2160</p>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { useSysSettingStore } from '@/stores/sysSettingStore.js'
const sysSettingStore = useSysSettingStore()

import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

import { mitter } from '@/eventbus/eventBus.js'

const emit = defineEmits(['addFile'])
const addFile = (file) => {
  emit('addFile', file)
}
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

let hasStarted = false;

const startUpload = (file) => {
  if (!checkVideoFile(file)) {
    return false;
  }

  if (!hasStarted) {
    hasStarted = true;
    mitter.emit("startUpload", proxy.Utils.getFileName(file.name));
  }

  return true;
};
</script>

<style lang="scss" scoped>
.uploader-start-panel {
  margin: 20px 200px;
  .uploader-start {
    border: 2px dashed #ccc;
    :deep(.el-upload-dragger) {
      border: none;
    }
    .upload-handler {
      color: #999;
      padding: 50px 0px;
      .icon-upload {
        font-size: 30px;
      }
      .info {
        margin: 20px 0px;
      }
      .upload-btn {
        color: #fff;
        margin: 20px auto;
        width: 200px;
        height: 44px;
        cursor: pointer;
        background: #00a1d6;
        border-radius: 4px;
        transition: background-color 0.3s ease;
        text-align: center;
        line-height: 40px;
        &:hover {
          opacity: 0.8;
        }
      }
    }
  }
  .upload-explain {
    margin-top: 20px;
    display: flex;
    justify-content: center;
    .item {
      color: var(--text3);
      padding: 0px 20px;
      cursor: pointer;
    }
  }
}
</style>
