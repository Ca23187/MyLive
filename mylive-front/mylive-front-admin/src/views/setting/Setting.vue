<template>
  <div class="setting-form">
    <el-form
      :model="formData"
      :rules="rules"
      ref="formDataRef"
      label-width="240px"
      @submit.prevent
    >
      <!--input输入-->
      <el-form-item label="Coins on Registration" prop="registerCoinCount">
        <el-input
          placeholder="Enter coin amount"
          v-model="formData.registerCoinCount"
          type="number"
          :min="1"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="Coins for Uploading Videos" prop="postVideoCoinCount">
        <el-input
          placeholder="Enter coins for uploading videos"
          v-model="formData.postVideoCoinCount"
          type="number"
          :min="1"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="Max Video Size" prop="videoSize">
        <el-input
          placeholder="Enter max video size"
          v-model="formData.videoSize"
          type="number"
          :min="1"
        >
          <template #suffix> MB </template>
        </el-input>
      </el-form-item>
      <el-form-item label="Max Episodes per Upload" prop="videoPartCount">
        <el-input
          placeholder="Enter max episodes per upload"
          v-model="formData.videoPartCount"
          type="number"
          :min="1"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="Daily Video Upload Limit" prop="videoCount">
        <el-input
          placeholder="Enter video upload limit"
          v-model="formData.videoCount"
          type="number"
          :min="1"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="Daily Comment Limit" prop="commentCount">
        <el-input
          placeholder="Enter comment limit"
          v-model="formData.commentCount"
          type="number"
          :min="1"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="Daily Danmaku Limit" prop="danmakuCount">
        <el-input
          placeholder="Enter danmaku limit"
          v-model="formData.danmakuCount"
          type="number"
          :min="1"
        >
        </el-input>
      </el-form-item>

      <el-form-item label="Registration Email Subject" prop="registerEmailTitle">
        <el-input
          placeholder="Enter registration email subject"
          v-model="formData.registerEmailTitle"
        />
      </el-form-item>

      <el-form-item label="Registration Email Content" prop="registerEmailContent">
        <el-input
          placeholder="Enter registration email content. Use %s for the verification code"
          v-model="formData.registerEmailContent"
          type="textarea"
          :rows="10"
        />
      </el-form-item>

      <!--input输入-->
      <el-form-item label="" prop="">
        <el-button type="primary" @click="saveSetting">Save</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue";
import { useRouter } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();

const formData = ref({});
const formDataRef = ref();
const rules = {
  registerCoinCount: [{ required: true, message: "Enter coins on registration" }],
  postVideoCoinCount: [{ required: true, message: "Enter coins for uploading videos" }],
  videoSize: [{ required: true, message: "Enter max video size" }],
  videoCount: [{ required: true, message: "Enter video upload limit" }],
  videoPartCount: [{ required: true, message: "Enter max episodes per upload" }],
  commentCount: [{ required: true, message: "Enter comment limit" }],
  danmakuCount: [{ required: true, message: "Enter danmaku limit" }],
  registerEmailTitle: [{ required: true, message: "Enter registration email subject" }],
  registerEmailContent: [{ required: true, message: "Enter registration email content." }],
};

const getSetting = async () => {
  let result = await proxy.Request({
    url: proxy.Api.getSetting,
  });
  if (!result) {
    return;
  }
  formData.value = result.data;
};
getSetting();

const saveSetting = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    let params = {};
    Object.assign(params, formData.value);
    let result = await proxy.Request({
      url: proxy.Api.saveSetting,
      params,
    });
    if (!result) {
      return;
    }
    proxy.Message.success("Saved successfully");
  });
};
</script>

<style lang="scss" scoped>
.setting-form {
  padding: 20px;
  width: 800px;
    :deep(.el-form-item__label) {
    text-align: left;
    justify-content: flex-start;
  }
}
</style>
