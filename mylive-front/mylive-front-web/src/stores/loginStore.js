import { defineStore } from "pinia";

const useLoginStore = defineStore("loginState", {
  state: () => {
    return {
      showLogin: false,
      userInfo: {},
      messageNoReadCount: 0,
      deviceId: null,
    };
  },

  getters: {
    isLogin(state) {
      return !!state.userInfo?.userId;
    },

    loginUserId(state) {
      return state.userInfo?.userId;
    },
  },

  actions: {
    setLogin(show) {
      this.showLogin = show;
    },

    saveUserInfo(info) {
      this.userInfo = info || {};
    },

    updateUserInfo(info) {
      this.userInfo = {
        ...this.userInfo,
        ...info,
      };
    },

    saveMessageNoReadCount(count) {
      this.messageNoReadCount = count || 0;
    },

    readMessageCount(count) {
      this.messageNoReadCount = Math.max(
        0,
        this.messageNoReadCount - Number(count || 0)
      );
    },

    saveDeviceId(deviceId) {
      this.deviceId = deviceId;
    },

    clearLoginInfo() {
      this.userInfo = {};
      this.messageNoReadCount = 0;
    },

    logout() {
      this.clearLoginInfo();
      this.setLogin(false);
    },
  },
});

export { useLoginStore };