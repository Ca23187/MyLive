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
            <el-button type="success" @click="loadDataList">Search</el-button>
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
      <template #slotComment="{ row }">
        <div class="comment-info">
          <a
            class="a-link nick-name"
            :href="`${proxy.webDomain}/user/${row.userId}`"
            target="_blank"
          >
            <Avatar v-if="row.avatar" :avatar="row.avatar" />
            <span v-else class="default-avatar">{{ row.nickname?.substring(0, 1) || '-' }}</span>
          </a>

          <div class="comment">
            <div class="user-line">
              <a
                class="a-link nick-name"
                :href="`${proxy.webDomain}/user/${row.userId}`"
                target="_blank"
              >
                {{ row.nickname || '-' }}
              </a>

              <template v-if="row.replyUserId">
                <span class="reply-text">&nbsp;replied to&nbsp;</span>
                <a
                  class="a-link nick-name"
                  :href="`${proxy.webDomain}/user/${row.replyUserId}`"
                  target="_blank"
                >
                  {{ row.replyNickname || '-' }}
                </a>
              </template>

              <span v-if="row.parentCommentId" class="tag">Reply</span>
            </div>

            <div class="content" v-html="renderCommentContent(row)"></div>

            <div v-if="row.imgPath" class="comment-img-wrap">
              <img class="comment-img" :src="row.imgPath" />
            </div>

            <div class="count-info">
              <span>Likes {{ row.likeCount ?? 0 }}</span>
              <span>Dislikes {{ row.dislikeCount ?? 0 }}</span>
              <span>Replies {{ row.replyCount ?? 0 }}</span>
              <span v-if="row.topType">Pinned</span>
            </div>

            <div class="time-info">
              <div class="time">{{ row.postedAt }}</div>
              <div
                class="iconfont icon-delete"
                title="Delete"
                @click="delComment(row.commentId)"
              ></div>
            </div>
          </div>
        </div>
      </template>

      <template #slotVideo="{ row }">
        <a
          :href="`${proxy.webDomain}/video/${row.videoId}`"
          target="_blank"
          class="a-link video-link"
        >
          <Cover
            v-if="row.videoCover"
            :source="row.videoCover"
          />
          <img
            v-else
            class="video-cover-empty"
            src=""
            alt=""
          />
          <div class="video-name">{{ row.videoTitle || '-' }}</div>
        </a>
      </template>

      <template #slotOperation="{ row }">
        <a
          href="javascript:void(0)"
          class="a-link"
          @click="delComment(row.commentId)"
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
    label: 'Comment',
    scopedSlots: 'slotComment',
  },
  {
    label: 'Video',
    scopedSlots: 'slotVideo',
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

const tableData = ref({
  list: [],
  pageNo: 1,
  pageSize: 15,
  totalCount: 0,
})

const loadDataList = async () => {
  const params = {
    pageNo: tableData.value.pageNo || 1,
    pageSize: tableData.value.pageSize || 15,
    ...searchForm.value,
  }

  const result = await proxy.Request({
    url: proxy.Api.loadComment,
    params,
  })

  if (!result) {
    return
  }

  Object.assign(tableData.value, result.data || {})
}

const delComment = (commentId) => {
  proxy.Confirm({
    message: 'Are you sure you want to delete this comment?',
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.delComment,
        params: {
          commentId,
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

const renderCommentContent = (data = {}) => {
  let content = resetHtmlContent(data.content || '')

  const mentionUsers = Array.isArray(data.mentionUsers) ? data.mentionUsers : []

  if (mentionUsers.length === 0) {
    return content
  }

  mentionUsers.forEach((user) => {
    if (!user || !user.nickname) {
      return
    }

    const reg = new RegExp(`@${escapeRegExp(user.nickname)}`, 'g')

    content = content.replace(
      reg,
      `<a class="comment-mention" href="${proxy.webDomain}/user/${user.userId}" target="_blank" rel="noopener noreferrer">@${user.nickname}</a>`
    )
  })

  return content
}

const resetHtmlContent = (data) => {
    if (!data) {
        return data;
    }
    data = data.replace(/\r\n/g, "<br>");
    data = data.replace(/\n/g, "<br>");
    return data;
}

const escapeRegExp = (str = '') => {
  return String(str).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
</script>

<style lang="scss" scoped>
.comment-info {
  display: flex;
  align-items: flex-start;

  .comment {
    margin-left: 10px;
    flex: 1;
    min-width: 0;
  }

  .user-line {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }

  .reply-text {
    color: var(--text3);
    font-size: 13px;
  }

  .tag {
    margin-left: 6px;
    padding: 1px 5px;
    border-radius: 3px;
    background: #eef3ff;
    color: #409eff;
    font-size: 12px;
  }

  .time-info {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: var(--text3);

    .iconfont {
      margin-left: 8px;
      font-size: 13px;
      cursor: pointer;
    }
  }
}

.nick-name {
  text-decoration: none;
}

.content {
  margin-top: 4px;
  line-height: 22px;
  word-break: break-all;

  :deep(.comment-mention) {
    color: #00aeec;
    text-decoration: none;
    cursor: pointer;

    &:hover {
      color: #00a1d6;
    }
  }
}

.comment-img-wrap {
  margin-top: 6px;
}

.comment-img {
  max-width: 120px;
  max-height: 120px;
  border-radius: 4px;
  object-fit: cover;
}

.count-info {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text3);

  span {
    margin-right: 12px;
  }
}

.video-link {
  display: block;
  text-decoration: none;
}

.video-name {
  color: var(--text3);
  font-size: 13px;
  margin-top: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
}

.default-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e5e5e5;
  color: #666;
  font-size: 14px;
}

.video-cover-empty {
  display: block;
  width: 120px;
  height: 70px;
  background: #eee;
  border-radius: 4px;
}
</style>