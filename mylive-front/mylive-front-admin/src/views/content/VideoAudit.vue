<template>
  <Dialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="600px"
    :showCancel="true"
    @close="dialogConfig.show = false"
  >
    <el-form
      :model="formData"
      :rules="rules"
      ref="formDataRef"
      label-width="140px"
      @submit.prevent
    >
      <!--input输入-->
      <el-form-item label="Review Decision" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="3">Approved</el-radio>
          <el-radio :value="4">Rejected</el-radio>
        </el-radio-group>
      </el-form-item>
      <!--input输入-->
      <el-form-item label="Reason" prop="reason" v-if="formData.status == 4">
        <el-input
          resize="none"
          type="textarea"
          :rows="4"
          clearable
          placeholder="Please enter a rejection reason"
          v-model="formData.reason"
          show-word-limit
          :maxlength="200"
        ></el-input>
      </el-form-item>
    </el-form>
  </Dialog>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue";
const { proxy } = getCurrentInstance();
import { useRoute, useRouter } from "vue-router";
const route = useRoute();
const router = useRouter();

const formData = ref({});
const formDataRef = ref();
const rules = {
  status: [{ required: true, message: "Please select a review result" }],
  reason: [{ required: true, message: "Please enter a rejection reason" }],
};

const dialogConfig = ref({
  show: false,
  title: "Review",
  buttons: [
    {
      type: "primary",
      text: "Confirm",
      click: (e) => {
        audit();
      },
    },
  ],
});

const show = (videoId, userId) => {
  dialogConfig.value.show = true;
  nextTick(() => {
    formDataRef.value.resetFields();
    formData.value = {
      videoId,
      userId,
    };
  });
};
defineExpose({
  show,
});

const emit = defineEmits(["reload"]);
const audit = async () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    let params = {};
    Object.assign(params, formData.value);
    let result = await proxy.Request({
      url: proxy.Api.reviewVideo,
      params,
    });
    if (!result) {
      return;
    }
    dialogConfig.value.show = false;
    proxy.Message.success("Review completed");
    emit("reload");
  });
};
</script>

<style lang="scss" scoped>
</style>
