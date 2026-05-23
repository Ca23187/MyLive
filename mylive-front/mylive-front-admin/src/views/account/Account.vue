<template>
  <div class="account-panel">
    <el-form
      class="login-panel"
      :model="formData"
      :rules="rules"
      ref="formDataRef"
    >
      <div class="login-title">Login</div>
      <!--input输入-->
      <el-form-item prop="email">
        <el-input
          size="large"
          clearable
          placeholder="Please enter your account"
          v-model="formData.account"
          maxLength="150"
        >
          <template #prefix>
            <span class="iconfont icon-account"></span>
          </template>
        </el-input>
      </el-form-item>
      <!--登录密码-->
      <el-form-item prop="password">
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
      <el-form-item prop="checkCode">
        <div class="check-code-panel">
          <el-input
            size="large"
            placeholder="Please enter the captcha"
            v-model="formData.checkCode"
            @keyup.enter="doSubmit"
          >
            <template #prefix>
              <span class="iconfont icon-checkcode"></span>
            </template>
          </el-input>
          <img
            :src="checkCodeInfo.checkCode"
            class="check-code"
            @click="changeCheckCode()"
          />
        </div>
      </el-form-item>
      <el-button type="primary" size="large" class="op-btn" @click="doSubmit">
        Login
      </el-button>
    </el-form>
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
} from "vue";
import { useRouter, useRoute } from "vue-router";
import md5 from "js-md5";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

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
changeCheckCode();

// 0:注册 1:登录
const formData = ref({});
const formDataRef = ref();
const rules = {
  account: [{ required: true, message: "Please enter your captcha" }],
  password: [{ required: true, message: "Please enter your password" }],
  checkCode: [{ required: true, message: "Please enter the captcha" }],
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
    let result = await proxy.Request({
      url: proxy.Api.login,
      params: params,
      errorCallback: () => {
        changeCheckCode();
      },
    });
    if (!result) {
      return;
    }
    router.push("/home");
    proxy.Message.success("Login successful");
    proxy.VueCookies.set("account", result.data);
  });
};
</script>

<style lang="scss">
.account-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh);
  background: url("../../assets/login-bg.png");
  background-position: center, center;
  background-size: 100% 100%;
  .login-panel {
    background-color: rgba(255, 255, 255, 0.8);
    padding: 30px;
    width: 400px;
    border-radius: 5px;
    .login-title {
      text-align: center;
      font-size: 16px;
      line-height: 40px;
    }
    .check-code-panel {
      width: 100%;
      display: flex;
      justify-content: space-between;
      .check-code {
        margin-left: 5px;
        cursor: pointer;
      }
    }
    .op-btn {
      width: 100%;
    }
  }
}
</style>