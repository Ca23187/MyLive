<template>
  <div
    class="header-fixed"
    :style="{
      'max-width': proxy.bodyMaxWidth + 'px',
      'min-width': proxy.bodyMinWidth + 'px',
    }"
  >
    <LayoutHeader theme="dark"></LayoutHeader>
  </div>
  <div
    class="user-home-body-container"
    :style="{
      'max-width': proxy.bodyMaxWidth + 'px',
      'min-width': proxy.bodyMinWidth + 'px',
      'background-image': `url(${proxy.Utils.getLocalImage(
        'uhome-bg/' + (userInfo.theme || 0) + '.png'
      )})`,
    }"
  >
    <div class="user-home-body">
      <!-- <div class="header">
        <img :src="proxy.Utils.getLocalImage('user-home-bg.png')" />
      </div> -->
      <div class="user-info-panel">
        <div class="avatar">
          <Avatar
            :width="90"
            :avatar="userInfo.avatar"
            :userId="userInfo.userId"
          ></Avatar>
        </div>
        <div class="user-info">
          <div class="user-name">
            <div>{{ userInfo.nickname }}</div>
            <div
              :class="[
                'iconfont',
                userInfo.gender == 0 ? 'icon-sexw' : 'icon-sexm',
              ]"
              v-if="userInfo.gender == 0 || userInfo.gender == 1"
            ></div>
            <div
              class="iconfont icon-edit"
              @click="updateUserInfo"
              v-if="myself"
            ></div>
          </div>
          <div class="introduction">
            {{ userInfo.profile }}
          </div>
        </div>
        <div class="follow-panel" v-if="!myself">
          <div
            class="btn-follow btn-cancel-follow"
            @click="unFollowUser(currentUserId)"
            v-if="userInfo.haveFollowed"
          >
            Unfollow
          </div>
          <div class="btn-follow" @click="followUser(currentUserId)" v-else>
            Follow
          </div>
        </div>
      </div>
      <div class="home-nav">
        <div class="nav-panel">
          <router-link
            :class="[
              'nav-item iconfont',
              item.icon,
              item.pathNames.includes(route.name) ? 'active' : '',
            ]"
            :to="item.path"
            v-for="item in navList"
            >{{ item.name }}
          </router-link>
        </div>
        <div class="search">
          <el-input
            placeholder="Search videos"
            style="width: 200px"
            v-model="videoTitle"
            @keyup.enter="searchVideo"
          >
            <template #suffix>
              <span class="iconfont icon-search"></span>
            </template>
          </el-input>
        </div>
        <div class="count-info">
          <router-link
            v-if="myself"
            class="count-item"
            :to="`/user/${currentUserId}/follow`"
          >
            <div class="title-info">Following</div>
            <div class="count-value">{{ userInfo.followCount }}</div>
          </router-link>
          <div class="count-item" v-else>
            <div class="title-info">Following</div>
            <div class="count-value">{{ userInfo.followCount }}</div>
          </div>

          <router-link
            v-if="myself"
            class="count-item"
            :to="`/user/${currentUserId}/fans`"
          >
            <div class="title-info">Followers</div>
            <div class="count-value">{{ userInfo.fanCount }}</div>
          </router-link>
          <div class="count-item" v-else>
            <div class="title-info">Followers</div>
            <div class="count-value">{{ userInfo.fanCount }}</div>
          </div>
        </div>
      </div>
      <div class="user-home-content">
        <router-view :key="route.fullPath"></router-view>
      </div>
    </div>
    <div
      class="change-them-btn"
      @click="selectTheme"
      v-if="
        loginStore.userInfo && userInfo.userId == loginStore.userInfo.userId
      "
    ></div>
  </div>
  <Account></Account>
  <UserInfoEdit ref="userInfoEditRef" @reload="loadUserInfo"></UserInfoEdit>
  <UserHomeTheme
    ref="userHomeThemeRef"
    @changeTheme="changeTheme"
  ></UserHomeTheme>
</template>

<script setup>
import UserHomeTheme from "./UserHomeTheme.vue";
import UserInfoEdit from "./UserInfoEdit.vue";
import Account from "@/views/account/Account.vue";
import LayoutHeader from "@/views/layout/LayoutHeader.vue";
import { computed, getCurrentInstance, provide, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
const { proxy } = getCurrentInstance();
const route = useRoute();
const router = useRouter();
import { useLoginStore } from "@/stores/loginStore.js";
const loginStore = useLoginStore();
const currentUserId = computed(() => route.params.userId);

const navList = computed(() => {
  return [
    {
      name: "Home",
      path: `/user/${currentUserId.value}`,
      icon: "icon-home",
      pathNames: ["uhome", "uhomeFans", "uhomeFollow"],
    },
    {
      name: "Videos",
      path: `/user/${currentUserId.value}/video`,
      icon: "icon-play",
      pathNames: ["uhomeMyVideo"],
    },
    {
      name: "Playlists",
      path: `/user/${currentUserId.value}/series`,
      icon: "icon-playlist",
      pathNames: ["uhomeSeries", "uhomeSeriesDetail"],
    },
    {
      name: "Saved",
      path: `/user/${currentUserId.value}/saved`,
      icon: "icon-collection",
      pathNames: ["saved"],
    },
  ];
});
//是否是自己
const myself = computed(() => {
  return (
    loginStore.isLogin &&
    String(loginStore.loginUserId) === String(currentUserId.value)
  );
});

const userInfo = ref({});
provide("userInfo", userInfo);
const loadUserInfo = async () => {
  let result = await proxy.Request({
    url: proxy.Api.uHomeGetUsesrInfo,
    params: {
      userId: currentUserId.value,
    },
  });
  if (!result) {
    return;
  }
  userInfo.value = result.data;
  let noticeInfo = result.data.noticeInfo;
  if (noticeInfo) {
    noticeInfo = noticeInfo.replace(/\r\n/g, "<br>");
    noticeInfo = noticeInfo.replace(/\n/g, "<br>");
    userInfo.value.noticeInfo = noticeInfo;
  }

  if (loginStore.isLogin && loginStore.loginUserId == currentUserId.value) {
    loginStore.updateUserInfo({
      avatar: result.data.avatar,
      nickname: result.data.nickname,
      gender: result.data.gender,
    });
  }
};
watch(
  () => route.params.userId,
  () => {
    loadUserInfo();
  },
  { immediate: true }
);
const userInfoEditRef = ref();
const updateUserInfo = () => {
  userInfoEditRef.value.show(userInfo.value);
};

const followUser = async (followUserId, changeCountType = 0, fn) => {
  if (!loginStore.isLogin) {
    loginStore.setLogin(true);
    return;
  }
  let result = await proxy.Request({
    url: proxy.Api.uHomeFollow,
    showLoading: true,
    params: {
      followUserId: followUserId,
    },
  });
  if (!result) {
    return;
  }
  if (changeCountType == 0) {
    userInfo.value.haveFollowed = true;
    userInfo.value.fanCount++;
  } else {
    userInfo.value.followCount++;
  }
  if (fn) {
    fn();
  }
};
const unFollowUser = async (followUserId, changeCountType = 0, fn) => {
  let result = await proxy.Request({
    url: proxy.Api.uHomeUnFollow,
    showLoading: true,
    params: {
      followUserId,
    },
  });
  if (!result) {
    return;
  }
  if (changeCountType == 0) {
    userInfo.value.haveFollowed = false;
    userInfo.value.fanCount--;
  } else {
    userInfo.value.followCount--;
  }
  if (fn) {
    fn();
  }
};

provide("unFollowUser", (followUserId, fn) => {
  unFollowUser(followUserId, 1, fn);
});

provide("followUser", (followUserId, fn) => {
  followUser(followUserId, 1, fn);
});

const videoTitle = ref();
const searchVideo = () => {
  router.push({
    path: `/user/${route.params.userId}/video`,
    query: {
      videoTitle: videoTitle.value,
    },
  });
};

const userHomeThemeRef = ref();
const selectTheme = () => {
  userHomeThemeRef.value.show(userInfo.value.theme);
};

const changeTheme = (theme) => {
  userInfo.value.theme = theme;
};
</script>

<style lang="scss" scoped>
.header-fixed {
  background: #fff;
  height: 64px;
  margin: 0px auto;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.user-home-body-container {
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  margin: 0px auto;
  .user-home-body {
    margin: 0px auto;
    position: relative;
    min-height: calc(100vh - 64px);
    width: 1300px;
    .header {
      height: 250px;
      overflow: hidden;
    }

    .user-info-panel {
      background: #fff;
      position: relative;
      padding: 5px 30px 5px 0px;
      border-radius: 0px 0px 5px 5px;
      display: flex;
      align-items: center;

      .avatar {
        position: absolute;
        left: 20px;
        top: -35px;
      }

      .user-info {
        flex: 1;
        color: var(--text);
        margin-left: 120px;

        .user-name {
          font-size: 20px;
          font-weight: bold;
          display: flex;
          align-items: center;

          .iconfont {
            font-weight: normal;
            margin-left: 10px;
            color: var(--text3);
            cursor: pointer;
          }

          .icon-sexw {
            font-size: 20px;
            color: #f25d8e;
          }

          .icon-sexm {
            font-size: 20px;
            color: #5fd4f2;
          }
        }

        .introduction {
          color: var(--text3);
          margin-top: 5px;
          min-height: 20px;
        }
      }

      .follow-panel {
        .btn-follow {
          background: #f25d8e;
          color: #fff;
          padding: 8px 25px;
          border-radius: 5px;
          cursor: pointer;

          &:hover {
            opacity: 0.7;
          }
        }

        .btn-cancel-follow {
          background: #fff;
          color: var(--text);
          border: 1px solid #ddd;
        }
      }
    }

    .home-nav {
      top: 0px;
      position: sticky;
      margin-top: 10px;
      background: #fff;
      border-radius: 5px;
      display: flex;
      align-items: center;
      padding: 0px 20px;
      z-index: 1000;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);

      .nav-panel {
        display: flex;
        align-items: center;

        .nav-item {
          text-decoration: none;
          margin-right: 35px;
          position: relative;
          display: flex;
          align-items: center;
          color: var(--text2);
          line-height: 65px;
          font-size: 14px;

          &::before {
            margin-right: 5px;
            font-size: 20px;
          }

          &:hover {
            color: var(--blue);

            &::after {
              content: "";
              border: 2px solid var(--blue);
              position: absolute;
              bottom: 0px;
              left: 0px;
              width: 100%;
            }
          }
        }

        .active {
          color: var(--blue);

          &::after {
            content: "";
            border: 2px solid var(--blue);
            position: absolute;
            bottom: 0px;
            left: 0px;
            width: 100%;
          }
        }
      }

      .search {
        margin-left: 20px;
        flex: 1;
      }

      .count-info {
        display: flex;
        text-align: center;
        font-size: 13px;

        .count-item {
          padding: 0px 10px;
          text-decoration: none;

          .title-info {
            color: var(--text3);
          }

          .count-value {
            margin-top: 5px;
          }
        }

        a.count-item {
          .title-info {
            color: var(--blue2);
          }

          color: var(--blue2);
        }
      }
    }

    .user-home-content {
      margin-top: 10px;
    }
  }
}

.change-them-btn {
  position: absolute;
  top: 64px;
  right: 0px;
  width: 58px;
  height: 49px;
  background-image: url("../../assets/theme-trigger.png");
  background-position: 0px 0px;
  background-repeat: no-repeat;
  cursor: pointer;
  &:hover {
    background-position: -522px 0px;
  }
}
</style>
