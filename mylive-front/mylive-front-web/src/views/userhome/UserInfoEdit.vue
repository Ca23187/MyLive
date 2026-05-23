<template>
  <Dialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="500px"
    @close="dialogConfig.show = false"
  >
    <el-form
      :model="formData"
      :rules="rules"
      ref="formDataRef"
      label-width="80px"
      @submit.prevent
    >
      <el-form-item label="UID" prop="">
        {{ formData.userId }}
      </el-form-item>
      <!--input输入-->
      <el-form-item label="Avatar" prop="avatar">
        <ImageCoverSelect
          :coverWidth="150"
          :cutWidth="150"
          :scale="1"
          :coverImage="formData.avatar"
        ></ImageCoverSelect>
      </el-form-item>
      <el-form-item label="Nickname" prop="nickname">
        <el-input
          clearable
          placeholder="Nickname"
          v-model="formData.nickname"
          :maxlength="20"
          show-word-limit
        ></el-input>
      </el-form-item>
      <!-- 单选 -->
      <el-form-item label="Gender" prop="gender">
        <el-radio-group v-model="formData.gender">
          <el-radio :value="0">Female</el-radio>
          <el-radio :value="1">Male</el-radio>
          <el-radio :value="2">Prefer not to say</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="Birthday" prop="birthday">
        <el-date-picker
          v-model="formData.birthday"
          type="date"
          placeholder="Select your birthday"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <!--input输入-->
      <el-form-item label="School" prop="school">
        <el-input
          clearable
          placeholder="School information"
          v-model="formData.school"
          :maxlength="150"
          show-word-limit
        ></el-input>
      </el-form-item>
      <!--textarea输入-->
      <el-form-item label="Bio" prop="profile">
        <el-input
          clearable
          placeholder="Bio"
          type="textarea"
          :rows="3"
          :maxlength="80"
          resize="none"
          show-word-limit
          v-model="formData.profile"
        ></el-input>
      </el-form-item>
      <el-form-item label="About" prop="noticeInfo">
        <el-input
          clearable
          placeholder="About"
          type="textarea"
          :rows="5"
          :maxlength="300"
          resize="none"
          show-word-limit
          v-model="formData.noticeInfo"
        ></el-input>
      </el-form-item>
    </el-form>
  </Dialog>
</template>

<script setup>
import { uploadImage } from "@/utils/Api.js";
import { ref, reactive, getCurrentInstance, nextTick, provide } from "vue";
const { proxy } = getCurrentInstance();
import { useRoute, useRouter } from "vue-router";
const route = useRoute();
const router = useRouter();
import { useLoginStore } from "@/stores/loginStore.js";
const loginStore = useLoginStore();

const dialogConfig = ref({
  show: false,
  title: "Edit Profile",
  buttons: [
    {
      type: "primary",
      text: "Confirm",
      click: (e) => {
        submitForm();
      },
    },
  ],
});
provide("cutImageCallback", ({ coverImage }) => {
  formData.value.avatar = coverImage;
});

const formData = ref({});
const formDataRef = ref();
const rules = {
  avatar: [{ required: true, message: "Please upload an avatar" }],
  nickname: [{ required: true, message: "Please enter a nickname" }],
  gender: [{ required: true, message: "Please select a gender" }],
};

const show = (data) => {
  dialogConfig.value.show = true;
  nextTick(() => {
    formDataRef.value.resetFields();
    formData.value = Object.assign({}, data);
  });
};
defineExpose({
  show,
});

const emit = defineEmits(["reload"]);
const submitForm = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }

    let params = {};
    Object.assign(params, formData.value);

    if (params.avatar instanceof File) {
      const avatar = await uploadImage(params.avatar);
      if (!avatar) {
        return;
      }
      params.avatar = avatar;
    }

    let result = await proxy.Request({
      url: proxy.Api.uHomeUpdateUserInfo,
      params,
    });

    if (!result) {
      return;
    }

    // 同步更新当前登录用户信息
    if (loginStore.isLogin && loginStore.loginUserId == params.userId) {
      loginStore.updateUserInfo({
        avatar: params.avatar,
        nickname: params.nickname,
        gender: params.gender,
      });
    }

    dialogConfig.value.show = false;
    proxy.Message.success("Updated successfully");
    emit("reload");
  });
};
</script>

<style lang="scss" scoped>
</style>
