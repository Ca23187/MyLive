<template>
  <div class="top-panel">
    <el-card>
      <el-form :model="searchForm" @submit.prevent>
        <el-row :gutter="10">
          <el-col :span="5">
            <el-form-item label="Video Title">
              <el-input
                clearable
                placeholder="Search videos"
                v-model="searchForm.videoTitleFuzzy"
              />
            </el-form-item>
          </el-col>

          <el-col :span="5">
            <el-button type="primary" @click="loadDataList">Search</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>

  <el-card class="table-data-card">
    <Table
      ref="tableInfoRef"
      :columns="columns"
      :fetch="loadDataList"
      :dataSource="tableData"
      :options="tableOptions"
      :extHeight="tableOptions.extHeight"
    >
      <template #slotAvatar="{ row }">
        <Avatar :avatar="row.avatar"></Avatar>
      </template>

      <template #slotNickname="{ row }">
        <a
          target="_blank"
          class="nick-name"
          :href="`${proxy.webDomain}/user/${row.userId}`"
        >
          {{ row.nickname || '-' }}
        </a>
      </template>

      <template #time="{ row }">
        {{ proxy.Utils.convertSecondsToHMS(Math.round(row.time || 0)) }}
      </template>

      <template #slotText="{ row }">
        <div class="danmaku-text">{{ row.text }}</div>
        <a
          target="_blank"
          class="video-info"
          :href="`${proxy.webDomain}/video/${row.videoId}`"
        >
          Video: {{ row.videoTitle }}
        </a>
      </template>

      <template #slotVideoCover="{ row }">
        <a
          :href="`${proxy.webDomain}/video/${row.videoId}`"
          target="_blank"
          class="a-link video-link"
        >
          <Cover :source="row.videoCover" />
        </a>
      </template>

      <template #slotOperation="{ row }">
        <a
          href="javascript:void(0)"
          class="a-link"
          @click="delDanmaku(row.danmakuId)"
        >
          Delete
        </a>
      </template>
    </Table>
  </el-card>
</template>

<script setup>
import Table from '@/components/Table.vue'
import { ref, reactive, getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()

const columns = [
  {
    label: 'Avatar',
    prop: 'avatar',
    width: 80,
    scopedSlots: 'slotAvatar',
  },
  {
    label: 'Sender',
    prop: 'nickname',
    width: 150,
    scopedSlots: 'slotNickname',
  },
  {
    label: 'Time',
    prop: 'time',
    scopedSlots: 'time',
    width: 100,
  },
  {
    label: 'Danmaku',
    prop: 'text',
    scopedSlots: 'slotText',
  },
  {
    label: 'Cover',
    prop: 'videoCover',
    width: 120,
    scopedSlots: 'slotVideoCover',
  },
  {
    label: 'Sent At',
    prop: 'postedAt',
    width: 180,
  },
  {
    label: 'Actions',
    prop: 'operation',
    width: 80,
    scopedSlots: 'slotOperation',
  },
]

const tableInfoRef = ref()

const tableOptions = reactive({
  extHeight: 0,
})

const searchForm = ref({})

const tableData = ref({})

const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  }

  Object.assign(params, searchForm.value)

  let result = await proxy.Request({
    url: proxy.Api.loadDanmaku,
    params,
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
        url: proxy.Api.delDanmaku,
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
.video-info,
.nick-name {
  margin-top: 5px;
  font-size: 12px;
  color: var(--text3);
  text-decoration: none;
}

.nick-name {
  font-size: 14px;
  color: var(--text2);
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.video-cover {
  width: 90px;
  height: 50px;
  border-radius: 4px;
  object-fit: cover;
}

.danmaku-text {
  margin-bottom: 4px;
}
</style>