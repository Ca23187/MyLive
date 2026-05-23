<template>
  <div class="top-panel">
    <el-card>
      <el-form :model="searchForm" label-width="100px" label-position="right">
        <el-row>
          <el-col :span="5">
            <el-form-item label="Video Title">
              <el-input
                class="password-input"
                v-model="searchForm.videoTitleFuzzy"
                clearable
                placeholder="Search videos"
                @keyup.enter="loadDataList"
              >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="Category">
              <el-cascader
                style="width: 100%"
                v-model="searchForm.categoryIdArray"
                :options="categoryList"
                :clearable="true"
                :props="{
                  value: 'categoryId',
                  label: 'categoryName',
                  checkStrictly: true,
                }"
              />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="Feature">
              <!-- 下拉框 -->
              <el-select
                clearable
                placeholder="Status"
                v-model="searchForm.recommendType"
              >
                <el-option :value="0" label="Unfeatured"></el-option>
                <el-option :value="1" label="Featured"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4" :style="{ paddingLeft: '10px' }">
            <el-button type="success" @click="loadDataList()">Search</el-button>
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
      <template #videoCover="{ index, row }">
        <div class="cover-info">
          <Cover :source="row.videoCover" :width="160"></Cover>
          <div class="duration">
            {{ proxy.Utils.convertSecondsToHMS(row.duration) }}
          </div>
        </div>
      </template>

      <template #videoTitle="{ index, row }">
        <div class="video-info">
          <div class="video-name">{{ row.videoTitle }}</div>
          <div class="user-name iconfont icon-upzhu">{{ row.nickname }}</div>
          <div class="video-count">
            <span class="iconfont icon-play-solid">{{ row.playCount }}</span>
            <span class="iconfont icon-like-solid">{{ row.likeCount }}</span>
            <span class="iconfont icon-danmu-solid">{{ row.danmakuCount }}</span>
            <span class="iconfont icon-comment-solid">{{
              row.commentCount
            }}</span>
            <span class="iconfont icon-toubi">{{ row.coinCount }}</span>
            <span class="iconfont icon-collection-solid">{{
              row.saveCount
            }}</span>
          </div>
        </div>
      </template>

      <template #statusName="{ row, index }">
        <span :style="{ color: statusMap[row.status] }">{{
          row.statusName
        }}</span>
      </template>

      <template #recommendType="{ row, index }">
        {{ row.recommendType == 1 ? "Featured" : "Unfeatured" }}
      </template>

      <template #slotOperation="{ index, row }">
        <div class="row-op-panel">
          <a
            class="a-link"
            href="javascript:void(0)"
            @click.prevent="showDetail(row)"
            >Details</a
          >
          <el-divider direction="vertical" />
          <template v-if="row.status == 2">
            <a class="a-link" href="javascript:void(0)" @click="audit(row)"
              >Review</a
            >
            <el-divider direction="vertical" />
          </template>
          <template v-if="row.status == 3">
            <a
              class="a-link"
              href="javascript:void(0)"
              @click="recommend(row)"
              >{{ row.recommendType == 1 ? "Unfeature" : "Feature" }}</a
            >
            <el-divider direction="vertical" />
          </template>
          <a
            class="a-link"
            href="javascript:void(0)"
            @click.prevent="delAccount(row)"
            >Delete</a
          >
        </div>
      </template>
    </Table>
  </el-card>
  <VideoDetail ref="videoDetailRef"></VideoDetail>
  <VideoAudit ref="auditRef" @reload="loadDataList"></VideoAudit>
</template>

<script setup>
import VideoAudit from "./VideoAudit.vue";
import VideoDetail from "./VideoDetail.vue";
import { ElMessageBox } from "element-plus";
import { getCurrentInstance, nextTick, ref } from "vue";
const { proxy } = getCurrentInstance();

const userInfo = ref(
  JSON.parse(sessionStorage.getItem("userInfo")) || { menuList: [] }
);

const searchForm = ref({});

const tableData = ref({});
const tableOptions = ref({
  extHeight: 0,
});

const statusMap = {
  0: "#e6a23c", //转码中
  1: "#f56c6c", //转码失败
  2: "#e6a23c", //待审核
  3: "#67c23a", //成功
  4: "#f56c6c", //审核失败
};

const columns = [
  {
    label: "Cover",
    prop: "videoCover",
    width: 220,
    scopedSlots: "videoCover",
  },
  {
    label: "Video",
    prop: "videoTitle",
    scopedSlots: "videoTitle",
  },
  {
    label: "Last Updated",
    prop: "lastUpdatedAt",
    width: 200,
  },
  {
    label: "Status",
    prop: "statusName",
    width: 100,
    scopedSlots: "statusName",
  },
  {
    label: "Feature",
    prop: "recommendType",
    width: 100,
    scopedSlots: "recommendType",
  },
  {
    label: "Actions",
    prop: "operation",
    width: 190,
    scopedSlots: "slotOperation",
  },
];

const tableInfoRef = ref();

const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  };
  Object.assign(params, searchForm.value);

  if (params.categoryIdArray && params.categoryIdArray.length == 2) {
    params.categoryId = params.categoryIdArray[1];
  } else if (params.categoryIdArray && params.categoryIdArray.length == 1) {
    params.parentCategoryId = params.categoryIdArray[0];
  }
  delete params.categoryIdArray;
  let result = await proxy.Request({
    url: proxy.Api.loadVideoPostList,
    params: params,
  });
  if (!result) {
    return;
  }
  Object.assign(tableData.value, result.data);
};

const categoryList = ref([]);
const loadCategory = async () => {
  let result = await proxy.Request({
    url: proxy.Api.loadCategory,
  });
  if (!result) {
    return;
  }
  categoryList.value = result.data;
};
loadCategory();

//详情
const videoDetailRef = ref();
const showDetail = (data) => {
  videoDetailRef.value.show(data);
};

//审核
const auditRef = ref();
const audit = (row) => {
  auditRef.value.show(row.videoId, row.userId);
};
//删除
//删除
const delAccount = async (data) => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `Please enter a reason for deleting "${data.videoTitle}"`,
      "Delete Video",
      {
        confirmButtonText: "Confirm",
        cancelButtonText: "Cancel",
        inputType: "textarea",
        inputPlaceholder: "Please enter a deletion reason",
        inputValidator: (value) => {
          if (!value || !value.trim()) {
            return "Deletion reason cannot be empty";
          }
          if (value.trim().length > 200) {
            return "Deletion reason cannot exceed 200 characters";
          }
          return true;
        },
      }
    );

    let result = await proxy.Request({
      url: proxy.Api.deleteVideo,
      params: {
        videoId: data.videoId,
        reason: reason.trim(),
      },
    });

    if (!result) {
      return;
    }

    proxy.Message.success("Operation successful");
    loadDataList();
  } catch (e) {
    // 用户取消，不处理
  }
};

const recommend = (data) => {
  const recommendName = data.recommendType == 0 ? "feature" : "unfeature";
  proxy.Confirm({
    message: `Are you sure you want to ${recommendName} "${data.videoTitle}"?`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.recommendVideo,
        params: {
          videoId: data.videoId,
        },
      });
      if (!result) {
        return;
      }
      proxy.Message.success("操作成功");
      loadDataList();
    },
  });
};
</script>

<style lang="scss" scoped>
.detail-tree-panel {
  height: calc(100vh - 273px);
  overflow: auto;
  width: 100%;
}
.cover-info {
  min-width: 0;
  width: 160px;
  position: relative;
  .duration {
    position: absolute;
    right: 0px;
    bottom: 0px;
    padding: 3px;
    border-radius: 5px 0px 5px 0px;
    background: rgba(0, 0, 0, 0.7);
    opacity: 0.8;
    color: #fff;
    font-size: 13px;
  }
}
.video-info {
  .user-name {
    margin-top: 5px;
    color: var(--text3);
    font-size: 14px;
    &::before {
      margin-right: 5px;
    }
  }
  .video-count {
    margin-top: 10px;
    color: var(--text3);
    display: flex;
    align-items: center;
    .iconfont {
      font-size: 14px;
      margin-right: 20px;
      &::before {
        font-size: 18px;
        margin-right: 5px;
      }
    }
  }
}
</style>
