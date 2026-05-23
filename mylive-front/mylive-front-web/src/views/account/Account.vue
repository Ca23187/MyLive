<template>
  <div>
    <Dialog
      :show="loginStore.showLogin"
      :buttons="dialogConfig.buttons"
      width="1000px"
      :showCancel="false"
      @close="closeDialog"
      :padding="0"
      :draggable="false"
      :top="100"
    >
      <div class="dialog-panel">
        <div class="bg">
          <img :src="proxy.Utils.getLocalImage('login_bg.png')" />
        </div>
        <el-form
          class="login-register"
          :model="formData"
          :rules="rules"
          ref="formDataRef"
        >
          <div class="tab-panel">
            <div :class="[opType == 0 ? '' : 'active']" @click="showPanel(1)">
              Login
            </div>
            <el-divider direction="vertical" />
            <div :class="[opType == 1 ? '' : 'active']" @click="showPanel(0)">
              Register
            </div>
          </div>
          <!--input输入-->
          <el-form-item prop="email">
            <el-input
              size="large"
              clearable
              placeholder="Please enter your email"
              v-model="formData.email"
              maxLength="150"
            >
              <template #prefix>
                <span class="iconfont icon-account"></span>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="checkCode">
            <div class="captcha-row">
              <!-- captcha input -->
              <el-input
                class="captcha-input"
                size="large"
                placeholder="Captcha"
                v-model="formData.checkCode"
              >
                <template #prefix>
                  <span class="iconfont icon-checkcode"></span>
                </template>
              </el-input>

              <!-- captcha image -->
              <img
                :src="checkCodeInfo.checkCode"
                class="check-code-img"
                @click="changeCheckCode"
              />

              <!-- send btn -->
              <el-button
                v-if="opType == 0"
                type="primary"
                class="send-btn"
                @click="sendEmailCode"
                :disabled="sendEmailCodeDisabled"
              >
                {{ sendEmailCodeText }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="emailCode" v-if="opType == 0">
            <el-input
              size="large"
              placeholder="Email verification code"
              v-model="formData.emailCode"
            >
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
            </el-input>
          </el-form-item>

          <!--登录密码-->
          <el-form-item prop="password" v-if="opType == 1">
            <el-input
              show-password
              size="large"
              placeholder="Please enter your password"
              v-model="formData.password"
            >
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
          </el-form-item>
          <!--注册-->
          <div v-if="opType == 0">
            <el-form-item prop="nickname" v-if="opType == 0">
              <el-input
                size="large"
                clearable
                placeholder="Please enter your nickname"
                v-model="formData.nickname"
                maxLength="20"
              >
                <template #prefix>
                  <span class="iconfont icon-account"></span>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="registerPassword">
              <el-input
                show-password
                type="password"
                size="large"
                placeholder="Please enter your password"
                v-model="formData.registerPassword"
              >
                <template #prefix>
                  <span class="iconfont icon-password"></span>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="reRegisterPassword">
              <el-input
                show-password
                type="password"
                size="large"
                placeholder="Please re-enter your password"
                v-model="formData.reRegisterPassword"
              >
                <template #prefix>
                  <span class="iconfont icon-password"></span>
                </template>
              </el-input>
            </el-form-item>
          </div>
          <el-form-item class="bottom-btn">
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              @click="doSubmit"
            >
              <span v-if="opType == 0">Register</span>
              <span v-if="opType == 1">Login</span>
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUpdated,
  watch
} from "vue";
import { useRouter, useRoute } from "vue-router";
import md5 from "js-md5";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

import { useLoginStore } from "@/stores/loginStore.js";
const loginStore = useLoginStore();

//验证码
const checkCodeInfo = ref({});
const changeCheckCode = async () => {
  let result = await proxy.Request({
    url: proxy.Api.checkCode,
  });
  if (!result) {
    return;
  }
  checkCodeInfo.value = result.data;
};

//登录，注册 弹出配置
const dialogConfig = ref({
  show: true,
});

const checkRePassword = (rule, value, callback) => {
  if (value !== formData.value.registerPassword) {
    callback(new Error(rule.message));
  } else {
    callback();
  }
};

// 0:注册 1:登录
const opType = ref(1);
const formData = ref({});
const formDataRef = ref();
const rules = {
  email: [
    { required: true, message: "Please enter your email" },
    {
      validator: proxy.Verify.email,
      message: "Please enter a valid email address",
    },
  ],
  password: [{ required: true, message: "Please enter your password" }],
  nickname: [{ required: true, message: "Please enter your nickname" }],
  registerPassword: [
    { required: true, message: "Please enter your password" },
    {
      validator: proxy.Verify.password,
      message:
        "Password must be 8-18 characters and include letters, numbers, or special characters",
    },
  ],
  reRegisterPassword: [
    { required: true, message: "Please re-enter your password" },
    {
      validator: checkRePassword,
      message: "The two passwords do not match",
    },
  ],
  checkCode: [{ required: true, message: "Please enter the captcha" }],
  emailCode: [
    { required: true, message: "Please enter email verification code" },
  ],
};

//重置表单
const resetForm = async () => {
  formData.value = {};
  await nextTick();
  formDataRef.value?.clearValidate();
  changeCheckCode();
};

// 登录、注册、重置密码  提交表单
const doSubmit = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    let params = {};
    Object.assign(params, formData.value);
    params.checkCodeKey = checkCodeInfo.value.checkCodeKey;
    //登录
    let result = await proxy.Request({
      url: opType.value == 0 ? proxy.Api.register : proxy.Api.login,
      params: params,
      errorCallback: () => {
        changeCheckCode();
      },
    });
    if (!result) {
      return;
    }
    //注册返回
    if (opType.value == 0) {
      proxy.Message.success("Registration successful, please log in");
      showPanel(1);
    } else if (opType.value == 1) {
      proxy.Message.success("Login successful");
      loginStore.setLogin(false);
      loginStore.saveUserInfo(result.data);
    }
  });
};

const closeDialog = () => {
  dialogConfig.value.show = false;
  loginStore.setLogin(false);
};

const showPanel = (type) => {
  opType.value = type;
  if (loginStore.showLogin) {
    resetForm();
  }
};

const sendEmailCodeText = ref("Send");
const sendEmailCodeDisabled = ref(false);

const sendEmailCode = async () => {
  if (!formData.value.email) {
    proxy.Message.warning("Please enter your email first");
    return;
  }

  if (!formData.value.checkCode) {
    proxy.Message.warning("Please enter the captcha first");
    return;
  }

  let result = await proxy.Request({
    url: proxy.Api.sendEmailCode,
    params: {
      email: formData.value.email,
      checkCodeKey: checkCodeInfo.value.checkCodeKey,
      checkCode: formData.value.checkCode,
    },
    errorCallback: () => {
      changeCheckCode();
    },
  });

  if (!result) {
    return;
  }

  proxy.Message.success("Verification code has been sent to your email");

  changeCheckCode();

  sendEmailCodeDisabled.value = true;
  let seconds = 60;
  sendEmailCodeText.value = `${seconds}s`;

  const timer = setInterval(() => {
    seconds--;
    sendEmailCodeText.value = `${seconds}s`;

    if (seconds <= 0) {
      clearInterval(timer);
      sendEmailCodeDisabled.value = false;
      sendEmailCodeText.value = "Send";
    }
  }, 1000);
};

watch(
  () => loginStore.showLogin,
  async (show) => {
    if (show) {
      opType.value = 1;
      formData.value = {};
      await nextTick();
      formDataRef.value?.clearValidate();
      changeCheckCode();
    }
  }
);

onMounted(() => {
  if (loginStore.showLogin) {
    changeCheckCode();
  }
});
</script>

<style lang="scss">
.dialog-panel {
  display: flex;
  align-items: center;
  justify-content: space-around;
  .bg {
    width: 450px;
    height: 580px;
    overflow: hidden;
    img {
      width: 100%;
    }
  }
  .login-register {
    width: 350px;
    .tab-panel {
      margin: 10px auto;
      display: flex;
      width: 130px;
      font-size: 18px;
      align-items: center;
      justify-content: space-around;
      cursor: pointer;
      .active {
        color: var(--blue2);
      }
    }
    .no-account {
      width: 100%;
      display: flex;
      justify-content: space-between;
    }
    .login-btn {
      width: 100%;
    }
    .bottom-btn {
      margin-bottom: 0px;
    }
  }
}

.check-code-panel {
  display: flex;
  align-items: center;
  width: 100%;

  .el-input {
    flex: 1;
  }

  .send-btn {
    margin-left: 10px;
    width: 100px;
    flex-shrink: 0;
  }

  .check-code {
    margin-left: 5px;
    cursor: pointer;
  }
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.check-code-img {
  width: 110px;
  height: 40px;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
}

.send-btn {
  width: 90px;
  flex-shrink: 0;
}
</style>