import axios from 'axios'
import { ElLoading } from 'element-plus'
import Message from '../utils/Message'
import { useLoginStore } from "@/stores/loginStore"
import router from "@/router"

const responseTypeJson = 'json'
let loading = null;

const instance = axios.create({
  withCredentials: true,
  baseURL: "/api",
  timeout: 60 * 1000,
});

// ===== 请求拦截 =====
instance.interceptors.request.use(
  (config) => {
    if (config.showLoading) {
      loading = ElLoading.service({
        lock: true,
        text: 'Loading...',
        background: 'rgba(0, 0, 0, 0.7)',
      });
    }
    return config;
  },
  (error) => {
    if (error.config?.showLoading && loading) {
      loading.close();
    }
    Message.error("Failed to send request");
    return Promise.reject("Failed to send request");
  }
);

// ===== 响应拦截 =====
instance.interceptors.response.use(
  (response) => {
    const { showLoading, errorCallback, showError = true, responseType } = response.config;

    if (showLoading && loading) {
      loading.close();
    }

    const responseData = response.data;

    if (responseType == "arraybuffer" || responseType == "blob") {
      return responseData;
    }

    if (responseData.code == 200) {
      return responseData;
    } else if (responseData.code == 404) {
      router.replace("/404");
      return Promise.reject({ showError: false });
    } else if (responseData.code == 901) {
      const loginStore = useLoginStore();

      loginStore.logout();
      loginStore.setLogin(true);

      if (router.currentRoute.value.meta.requiresLogin === true) {
        router.replace("/");
      }

      return Promise.reject({ showError: false });
    } else {
      if (errorCallback) {
        errorCallback(responseData);
      }

      return Promise.reject({
        showError,
        msg: responseData.info,
      });
    }
  },
  (error) => {
    if (error.config?.showLoading && loading) {
      loading.close();
    }

    const status = error.response?.status;

    if (status === 404) {
      router.replace("/404");
      return Promise.reject({ showError: false });
    }

    return Promise.reject({
      showError: true,
      msg: status ? `Request error: ${status}` : "Network error",
    });
  }
);

// ===== 核心请求方法 =====
const request = (config) => {
  const {
    url,
    params = {},
    dataType, // json | form | auto
    showLoading = false,
    responseType = responseTypeJson,
    showError = true,
  } = config

  let requestData = null
  let headers = {
    'X-Requested-With': 'XMLHttpRequest',
  }

  // ===== 自动判断是否包含文件 =====
  const hasFile = Object.values(params).some(
    val => val instanceof File || val instanceof Blob
  )

  // ===== 1️⃣ JSON模式 =====
  if (dataType === 'json') {
    requestData = params
    headers['Content-Type'] = 'application/json'
  }

  // ===== 2️⃣ 文件上传（FormData）=====
  else if (dataType === 'form' || hasFile) {
    const formData = new FormData()
    for (let key in params) {
      if (params[key] !== undefined && params[key] !== null) {
        formData.append(key, params[key])
      }
    }
    requestData = formData
    // ❗不要手动设置 Content-Type，让浏览器自动加 boundary
  }

  // ===== 3️⃣ 默认（最关键：改这里）=====
  else {
    // 👉 用 URLSearchParams 替代 FormData（解决你这次的坑）
    const urlParams = new URLSearchParams()
    for (let key in params) {
      if (params[key] !== undefined && params[key] !== null) {
        urlParams.append(key, params[key])
      }
    }
    requestData = urlParams
    headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8'
  }

  return instance.post(url, requestData, {
    responseType,
    headers,
    showLoading,
    errorCallback: config.errorCallback,
    showError,
    onUploadProgress: (event) => {
      if (config.uploadProgressCallback) {
        config.uploadProgressCallback(event)
      }
    }
  }).catch(error => {
    if (error.showError) {
      Message.error(error.msg)
    }
    return null
  })
}

export default request;