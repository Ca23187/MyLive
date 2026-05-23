<template>
  <div class="danmu-panel">
    <VideoSearchSelect @loadData="loadData4VideoSelect"></VideoSearchSelect>

    <Table
      ref="tableInfoRef"
      :columns="columns"
      :fetch="loadDataList"
      :dataSource="tableData"
      :options="tableOptions"
      :extHeight="tableOptions.extHeight"
    >
      <!-- 弹幕信息 -->
      <template #slotDanmu="{ index, row }">
        <div class="danmu-info">
          <div class="content">
            {{ row.text }}
          </div>

          <div class="time-info">
            <span class="play-time">
              Playback Time: 
              {{ proxy.Utils.convertSecondsToHMS(Math.round(row.time)) }}
            </span>

            <span class="post-time">
              {{ row.postedAt }}
            </span>

            <span
              class="iconfont icon-delete"
              @click="delDanmaku(row.danmakuId)"
            ></span>
          </div>
        </div>
      </template>

      <!-- 视频信息 -->
      <template #slotVideo="{ index, row }">
        <router-link
          :to="`/video/${row.videoId}`"
          target="_blank"
          class="a-link video-wrapper"
        >
          <Cover :source="row.videoCover"></Cover>

          <div class="video-name">
            {{ row.videoTitle }}
          </div>
        </router-link>
      </template>
    </Table>
  </div>
</template>
<script setup>
import VideoSearchSelect from './VideoSerchSelect.vue'
import Table from '@/components/Table.vue'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const currentVideoId = ref(route.query.videoId)
const loadData4VideoSelect = (videoId) => {
  currentVideoId.value = videoId
  loadDataList()
}

const columns = [
  {
    label: 'Danmaku',
    scopedSlots: 'slotDanmu',
  },
  {
    label: 'Video',
    scopedSlots: 'slotVideo',
    width: 150,
  },
]

const tableInfoRef = ref()
const tableOptions = ref({
  extHeight: 10,
})

const tableData = ref({})
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    videoId: currentVideoId.value,
  }
  let result = await proxy.Request({
    url: proxy.Api.ucLoadDanmaku,
    params: params,
  })
  if (!result) {
    return
  }
  Object.assign(tableData.value, result.data)
}

const delDanmaku = (danmakuId) => {
  proxy.Confirm({
    message: 'Are you sure you want to delete this danmaku?',
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.ucDelDanmaku,
        params: {
          danmakuId,
        },
      })
      if (!result) {
        return
      }
      proxy.Message.success('Deleted successfully')
      loadDataList()
    },
  })
}
</script>

<style lang="scss" scoped>
.danmu-panel {
  .danmu-info {
    .content {
      font-size: 14px;
      line-height: 22px;
      color: var(--text1);
      word-break: break-all;
    }

    .time-info {
      display: flex;
      align-items: center;
      margin-top: 8px;
      font-size: 12px;
      color: var(--text3);

      .play-time {
        margin-right: 15px;
      }

      .post-time {
        margin-right: 10px;
      }

      .iconfont {
        cursor: pointer;
        font-size: 13px;

        &:hover {
          color: #ff4d4f;
        }
      }
    }
  }

  .video-wrapper {
    display: block;
    text-decoration: none;
  }

  .video-name {
    margin-top: 5px;
    font-size: 13px;
    color: var(--text3);

    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
