<template>
  <div class="top-panel">
    <el-card>
      <el-form :model="searchForm" @submit.prevent>
        <el-row :gutter="10">
          <el-col :span="5">
            <el-form-item label="Nickname">
              <el-input clearable placeholder="Search users" v-model="searchForm.nicknameFuzzy"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="Status" prop="">
              <el-select clearable placeholder="Status" v-model="searchForm.status">
                <el-option :value="0" label="Disabled"></el-option>
                <el-option :value="1" label="Active"></el-option>
              </el-select>
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
    <Table ref="tableInfoRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData" :options="tableOptions"
      :extHeight="tableOptions.extHeight">
      <template #slotAvatar="{ index, row }">
        <Avatar :avatar="row.avatar"></Avatar>
      </template>

      <template #slotNickname="{ index, row }">
        <a class="nick-name" :href="`${proxy.webDomain}/user/${row.userId}`">{{
          row.nickname
        }}</a>
      </template>

      <template #slotCreatedTime="{ index, row }">
        <div>Joined: {{ proxy.Utils.formatDate(row.createdAt) }}</div>
        <div>Last Login: {{ proxy.Utils.formatDate(row.lastLoginAt) }}</div>
      </template>

      <template #slotCoin="{ index, row }">
        <div>Current {{ row.currentCoinCount }}</div>
        <div>Total {{ row.totalCoinCount }}</div>
      </template>

      <template #slotStatus="{ row }">
        <el-tag v-if="row.status == 1" type="success">
          Active
        </el-tag>

        <el-tag v-else type="danger">
          Disabled
        </el-tag>
      </template>

      <template #slotOperation="{ index, row }">
        <a href="javascript:void(0)" class="a-link" @click="changeStatus(row)">{{ row.status == 0 ? "Activate" : "Disable" }}</a>
      </template>
    </Table>
  </el-card>
</template>

<script setup>
import Table from '@/components/Table.vue'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()

const SEX_MAP = {
  0: 'Female',
  1: 'Male',
  2: 'Private',
}

const columns = [
  {
    label: 'Avatar',
    prop: 'avatar',
    width: 80,
    scopedSlots: 'slotAvatar',
  },
  {
    label: 'Nickname',
    prop: 'nickname',
    width: 150,
    scopedSlots: 'slotNickname',
  },
  {
    label: 'Email',
    prop: 'email',
    width: 150,
  },
  {
    label: 'Birthday',
    prop: 'birthday',
    width: 150,
  },
  {
    label: 'Joined',
    prop: 'createdTime',
    scopedSlots: 'slotCreatedTime',
    width: 200,
  },
  {
    label: 'Last IP',
    prop: 'lastLoginIp',
    width: 150,
  },
  {
    label: 'Bio',
    prop: 'profile',
  },
  {
    label: 'Coins',
    prop: 'coin',
    scopedSlots: 'slotCoin',
  },
  {
    label: 'Status',
    prop: 'status',
    scopedSlots: 'slotStatus',
  },
  {
    label: 'Actions',
    prop: 'operation',
    width: 80,
    scopedSlots: 'slotOperation',
  },
]

const tableInfoRef = ref()
const tableOptions = ref({
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
    url: proxy.Api.loadUser,
    params: params,
  })
  if (!result) {
    return
  }
  Object.assign(tableData.value, result.data)
}

const changeStatus = (row) => {
  proxy.Confirm({
    message: `Are you sure you want to ${row.status == 0 ? 'activate' : 'disable'} this user?`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.changeStatus,
        params: {
          userId: row.userId,
          status: row.status == 0 ? 1 : 0,
        },
      })
      if (!result) {
        return
      }
      proxy.Message.success('Operation successful')
      loadDataList()
    },
  })
}
</script>

<style lang="scss" scoped>
.nick-name {
  margin-top: 5px;
  font-size: 12px;
  text-decoration: none;
  font-size: 14px;
  color: var(--text2);
}
</style>
