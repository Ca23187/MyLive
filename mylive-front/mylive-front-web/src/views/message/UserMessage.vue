<template>
  <div class="message-panel">
    <div class="left-panel">
      <div class="message-title">Message Center</div>

      <div
        v-for="item in messageNav"
        :key="item.messageTypeCode"
        :class="[
          'message-part',
          route.params.messageType === item.messageTypeCode ? 'active' : '',
        ]"
        @click="selectMessageType(item)"
      >
        <div :class="['iconfont', item.icon]">{{ item.name }}</div>

        <div class="message-count" v-if="item.noReadCount > 0">
          {{ item.noReadCount > 99 ? "99+" : item.noReadCount }}
        </div>
      </div>
    </div>

    <div class="right-panel">
      <div class="home" v-if="!route.params.messageType">
        <div class="iconfont icon-message"></div>
      </div>

      <div class="message-list" v-else>
        <DataList :dataSource="dataSource" @loadData="loadDataList">
          <template #default="{ data }">
            <MessageItem
              :data="data"
              @delMessage="delMessage"
            />
          </template>
        </DataList>
      </div>
    </div>
  </div>
</template>

<script setup>
import MessageItem from "./MessageItem.vue";
import { ref, getCurrentInstance, onMounted, watch, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useLoginStore } from "@/stores/loginStore.js";

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const loginStore = useLoginStore();

/**
 * 后端真实消息类型
 */
const MESSAGE_TYPE = {
  SYS: 1,
  VIDEO_DELETE: 2,
  VIDEO_LIKE: 3,
  VIDEO_SAVE: 4,
  VIDEO_COIN: 5,
  VIDEO_COMMENT: 6,
  COMMENT_REPLY: 7,
  COMMENT_LIKE: 8,
  USER_FOLLOW: 9,
  COMMENT_MENTION: 10,
};

/**
 * 前端消息中心分组
 *
 * 注意：
 * messageTypeCode：路由分类
 * messageTypeList：后端真实 messageType 集合
 */
const messageNav = ref([
  {
    name: "System Notifications",
    messageTypeCode: "sys",
    messageTypeList: [MESSAGE_TYPE.SYS, MESSAGE_TYPE.VIDEO_DELETE],
    noReadCount: 0,
    icon: "icon-sys-message",
  },
  {
    name: "Likes",
    messageTypeCode: "like",
    messageTypeList: [MESSAGE_TYPE.VIDEO_LIKE, MESSAGE_TYPE.COMMENT_LIKE],
    noReadCount: 0,
    icon: "icon-good",
  },
  {
    name: "Saves",
    messageTypeCode: "save",
    messageTypeList: [MESSAGE_TYPE.VIDEO_SAVE],
    noReadCount: 0,
    icon: "icon-collection",
  },
  {
    name: "Coins",
    messageTypeCode: "coin",
    messageTypeList: [MESSAGE_TYPE.VIDEO_COIN],
    noReadCount: 0,
    icon: "icon-toubi",
  },
  {
    name: "Comments & Mentions",
    messageTypeCode: "comment",
    messageTypeList: [
      MESSAGE_TYPE.VIDEO_COMMENT,
      MESSAGE_TYPE.COMMENT_REPLY,
      MESSAGE_TYPE.COMMENT_MENTION,
    ],
    noReadCount: 0,
    icon: "icon-comment",
  },
  {
    name: "New Followers",
    messageTypeCode: "follow",
    messageTypeList: [MESSAGE_TYPE.USER_FOLLOW],
    noReadCount: 0,
    icon: "icon-user",
  },
]);

const curMessageNav = ref(null);

const dataSource = ref({
  list: [],
  pageNo: 1,
  pageSize: 15,
  pageTotal: 0,
  totalCount: 0,
});

const loginUserId = computed(() => {
  return loginStore.loginUserId;
});

const resetMessagePage = () => {
  curMessageNav.value = null;

  dataSource.value = {
    list: [],
    pageNo: 1,
    pageSize: 15,
    pageTotal: 0,
    totalCount: 0,
  };

  messageNav.value.forEach((item) => {
    item.noReadCount = 0;
  });
};

const checkLogin = () => {
  if (!loginStore.isLogin) {
    resetMessagePage();
    router.replace("/");
    loginStore.setLogin(true);
    return false;
  }

  return true;
};

const getNavByCode = (messageTypeCode) => {
  return messageNav.value.find((item) => {
    return item.messageTypeCode === messageTypeCode;
  });
};

const selectMessageType = async (item) => {
  curMessageNav.value = item;

  router.push(`/message/${item.messageTypeCode}`);

  await readAll(item);

  dataSource.value = {
    list: [],
    pageNo: 1,
    pageSize: 15,
    pageTotal: 0,
    totalCount: 0,
  };

  loadDataList();
};

/**
 * 加载消息列表
 *
 * 推荐后端支持参数：
 * messageTypeList=1,2
 *
 * 如果你后端目前只支持单个 messageType，
 * 这里需要后端改成支持多个类型查询。
 */
const loadDataList = async () => {
  if (!checkLogin()) {
    return;
  }

  if (!curMessageNav.value) {
    return;
  }

  const params = {
    pageNo: dataSource.value.pageNo,
    messageTypes: curMessageNav.value.messageTypeList.join(","),
  };

  const result = await proxy.Request({
    url: proxy.Api.loadUserMessage,
    params,
  });

  if (!result) {
    return;
  }

  dataSource.value = result.data;
};

const delMessage = (messageId) => {
  if (!checkLogin()) {
    return;
  }

  proxy.Confirm({
    message: "Are you sure you want to delete this message?",
    okfun: async () => {
      if (!checkLogin()) {
        return;
      }

      const result = await proxy.Request({
        url: proxy.Api.delMessage,
        params: {
          messageId,
        },
      });

      if (!result) {
        return;
      }

      loadDataList();
      getNoReadCountGroup();
    },
  });
};

/**
 * 获取未读数量
 *
 * 推荐后端返回真实 messageType 维度：
 * [
 *   { messageType: 1, messageCount: 2 },
 *   { messageType: 3, messageCount: 5 }
 * ]
 *
 * 前端再按 messageTypeList 聚合到左侧分组。
 */
const getNoReadCountGroup = async () => {
  if (!checkLogin()) {
    return;
  }

  const result = await proxy.Request({
    url: proxy.Api.getNoReadCountGroup,
  });

  if (!result) {
    return;
  }

  messageNav.value.forEach((nav) => {
    nav.noReadCount = 0;

    nav.messageTypeList.forEach((messageType) => {
      const messageTypeData = result.data.find((item) => {
        return item.messageType === messageType;
      });

      if (messageTypeData) {
        nav.noReadCount += messageTypeData.messageCount || 0;
      }
    });
  });
};

/**
 * 全部已读
 *
 * 推荐后端支持 messageTypeList=1,2
 */
const readAll = async (item) => {
  if (!checkLogin()) {
    return;
  }

  if (!item || item.noReadCount === 0) {
    return;
  }

  const result = await proxy.Request({
    url: proxy.Api.readAll,
    params: {
      messageTypes: item.messageTypeList.join(","),
    },
  });

  if (!result) {
    return;
  }

  loginStore.readMessageCount(item.noReadCount);
  item.noReadCount = 0;
};

const initByRoute = () => {
  if (!checkLogin()) {
    return;
  }

  if (!route.params.messageType) {
    curMessageNav.value = null;
    return;
  }

  const nav = getNavByCode(route.params.messageType);

  if (!nav) {
    curMessageNav.value = null;
    router.replace("/message");
    return;
  }

  curMessageNav.value = nav;

  dataSource.value = {
    list: [],
    pageNo: 1,
    pageSize: 15,
    pageTotal: 0,
    totalCount: 0,
  };

  loadDataList();
};

watch(
  () => route.params.messageType,
  () => {
    initByRoute();
  }
);

watch(
  () => loginUserId.value,
  (newUserId, oldUserId) => {
    if (!newUserId) {
      resetMessagePage();
      router.replace("/");
      return;
    }

    if (oldUserId && newUserId !== oldUserId) {
      resetMessagePage();
      getNoReadCountGroup();
      initByRoute();
    }
  }
);

onMounted(() => {
  if (!checkLogin()) {
    return;
  }

  getNoReadCountGroup();
  initByRoute();
});
</script>

<style lang="scss" scoped>
.message-panel {
  display: flex;
  height: calc(100vh - 180px);
  width: 1200px;
  margin: 0 auto;

  .left-panel {
    background-image: url(../../assets/creation_bg.png);
    background-repeat: no-repeat;
    background-size: 100% auto;
    background-color: #fff;
    width: 280px;
    border-radius: 5px;
    padding: 20px 10px;
    border: 1px solid #ddd;
    margin: 10px 0;

    .message-title {
      font-size: 20px;
      line-height: 45px;
      padding-left: 10px;
      font-weight: 600;
      color: #262626;
    }

    .message-part {
      cursor: pointer;
      line-height: 50px;
      border-radius: 5px;
      padding: 0 10px;
      display: flex;
      align-items: center;
      font-size: 14px;

      &:hover {
        background: #f8f8f8;
      }

      .iconfont {
        flex: 1;

        &::before {
          margin-right: 5px;
        }
      }

      .message-count {
        background: #f56c6c;
        color: #fff;
        padding: 0 4px;
        height: 20px;
        min-width: 20px;
        line-height: 20px;
        display: inline-block;
        border-radius: 5px;
        text-align: center;
        margin-left: 5px;
        font-size: 12px;
      }
    }

    .active {
      color: var(--blue);
      background: #f8f8f8;
    }
  }

  .right-panel {
    flex: 1;
    margin: 10px 0 10px 10px;
    background: #fff;
    border-radius: 5px;
    border: 1px solid #ddd;

    .home {
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;

      .iconfont {
        font-size: 200px;
        color: #ececec;
      }
    }

    .message-list {
      height: 100%;
      padding: 10px;
      overflow: auto;
    }
  }
}
</style>