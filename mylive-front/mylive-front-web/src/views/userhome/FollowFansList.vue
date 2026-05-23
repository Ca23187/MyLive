<template>
  <div class="body-container">
    <div class="body-title">
      {{ route.name == "uhomeFollow" ? "Following" : "Followers" }}
    </div>
    <DataList :dataSource="dataSource" @loadData="loadDataList">
      <template #default="{ data }">
        <div class="data-item">
          <Avatar
            :avatar="data.otherAvatar"
            :userId="data.otherUserId"
          ></Avatar>
          <div class="user-info">
            <div class="nick-name">
              <router-link
                :to="`/user/${data.otherUserId}`"
                target="_blank"
                class="a-link"
              >
                {{ data.otherNickname }}
              </router-link>
            </div>
            <div class="introduction">
              {{ data.otherProfile || "This user hasn't added a bio yet" }}
            </div>
          </div>
          <div class="op-btns">
            <div v-if="data.followType == 1" class="follow-eachother">
              Following Each Other
            </div>
            <el-button
              link
              type="primary"
              @click="unFollow(data.otherUserId)"
              v-if="data.followType == 1"
            >
              Unfollow
            </el-button>

            <el-button
              type="primary"
              v-if="data.followType == 0"
              @click="follow(data.otherUserId)"
            >
              Follow
            </el-button>
          </div>
        </div>
      </template>
    </DataList>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, watch, inject } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useLoginStore } from "@/stores/loginStore.js";

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const loginStore = useLoginStore();

const dataSource = ref({});

const checkLogin = () => {
  if (!loginStore.isLogin) {
    loginStore.setLogin(true);
    router.replace("/");
    return false;
  }

  return true;
};

const loadDataList = async () => {
  if (!checkLogin()) {
    return;
  }

  let params = {
    pageNo: dataSource.value.pageNo,
    pageSize: dataSource.value.pageSize,
  };

  let result = await proxy.Request({
    url:
      route.name == "uhomeFollow"
        ? proxy.Api.uHomeFollowList
        : proxy.Api.uHomeFanList,
    params,
  });

  if (!result) {
    return;
  }

  dataSource.value = result.data;
};

const unFollowUser = inject("unFollowUser");

const unFollow = (otherUserId) => {
  if (!checkLogin()) {
    return;
  }

  unFollowUser(otherUserId, () => {
    const item = dataSource.value.list.find(
      (item) => item.otherUserId === otherUserId
    );

    if (item) {
      item.followType = 0;
    }
  });
};

const followUser = inject("followUser");
const follow = (otherUserId) => {
  if (!checkLogin()) {
    return;
  }

  followUser(otherUserId, () => {
    const item = dataSource.value.list.find(
      (item) => item.otherUserId === otherUserId
    );

    if (item) {
      item.followType = 1;
    }
  });
};

watch(
  () => route.name,
  (newVal) => {
    if (newVal == "uhomeFollow" || newVal == "uhomeFans") {
      loadDataList();
    }
  },
  { immediate: true }
);
</script>
<style lang="scss" scoped>
.body-container {
  background: #ffff;
  padding: 20px;
  border-radius: 5px;
  .body-title {
    font-size: 18px;
    color: #6d757a;
    border-bottom: 1px solid #e5e9ef;
    padding: 0px 0px 10px 0px;
  }
  .data-item {
    display: flex;
    align-items: center;
    padding: 10px;
    .user-info {
      flex: 1;
      margin: 0px 10px;
      .introduction {
        margin-top: 10px;
        font-size: 13px;
        color: #6d757a;
        overflow: hidden;
        white-space: nowrap;
        width: 100%;
        text-overflow: ellipsis;
        padding-right: 10px;
      }
    }
    .op-btns {
      display: flex;
      align-items: center;
      .follow-eachother {
        margin-right: 10px;
        color: var(--text3);
      }
    }
  }
}
</style>
