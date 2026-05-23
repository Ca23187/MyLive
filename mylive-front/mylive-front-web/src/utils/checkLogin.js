// src/utils/checkLogin.js
import router from "@/router";
import { useLoginStore } from "@/stores/loginStore";

export const checkLogin = (options = {}) => {
  const {
    redirect = true,
    showLogin = true,
    redirectPath = "/",
  } = options;

  const loginStore = useLoginStore();

  if (loginStore.isLogin) {
    return true;
  }

  if (showLogin) {
    loginStore.setLogin(true);
  }

  if (redirect) {
    router.replace(redirectPath);
  }

  return false;
};