<template>
  <div class="upload-video-panel">
    <VideoUploader ref="videoUploaderRef"> </VideoUploader>
    <div v-if="startUpload" class="video-form">
      <el-form
        :model="formData"
        :rules="rules"
        ref="formDataRef"
        label-width="80px"
        @submit.prevent
      >
        <el-form-item label="Cover" prop="videoCover">
          <ImageCoverSelect
            :coverWidth="200"
            :cutWidth="680"
            :scale="0.6"
            :coverImage="formData.videoCover"
          >
          </ImageCoverSelect>
        </el-form-item>
        <!--input输入-->
        <el-form-item label="Title" prop="videoTitle">
          <el-input
            clearable
            placeholder="Enter title"
            v-model="formData.videoTitle"
            maxlength="100"
            show-word-limit
          ></el-input>
        </el-form-item>
        <!-- 单选 -->
        <el-form-item label="Type" prop="postType">
          <el-radio-group v-model="formData.postType">
            <el-radio :value="0">Original</el-radio>
            <el-radio :value="1">Repost</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="" prop="originInfo" v-if="formData.postType == 1">
          <el-input
            clearable
            placeholder="Please provide the original source, date, and location for reposted videos (e.g. reposted from https://xxx.com/yyyy). Proper attribution helps your video pass review faster."
            v-model="formData.originInfo"
            maxlength="200"
            show-word-limit
          ></el-input>
        </el-form-item>
        <el-form-item label="Tags" prop="tags">
          <TagInput v-model="formData.tags"></TagInput>
        </el-form-item>
        <el-form-item label="Category" prop="categoryArray">
          <el-cascader
            v-model="formData.categoryArray"
            :options="categoryStore.categoryList"
            :props="{ value: 'categoryId', label: 'categoryName' }"
          />
        </el-form-item>

        <!--textarea输入-->
        <el-form-item label="Description" prop="introduction">
          <el-input
            clearable
            placeholder="Add more details to help more people discover your video :)"
            type="textarea"
            :rows="5"
            :maxlength="2000"
            resize="none"
            show-word-limit
            v-model="formData.introduction"
          ></el-input>
        </el-form-item>
        <el-form-item label="Interaction Settings">
          <el-checkbox v-model="formData.disableDanmaku">
            Disable Danmaku
          </el-checkbox>
          <el-checkbox v-model="formData.disableComment">
            Disable Comments
          </el-checkbox>
        </el-form-item>
        <el-form-item label="">
          <el-button type="primary" @click="submitForm">Publish Now</el-button>
          <el-button @click="router.push('/ucenter/video')">Cancel</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { useCategoryStore } from "@/stores/categoryStore.js";
const categoryStore = useCategoryStore();
import TagInput from "./TagInput.vue";
import VideoUploader from "./VideoUploader.vue";
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUnmounted,
  watch,
  provide,
  inject,
} from "vue";

import { uploadImage } from "@/utils/Api.js";

const { proxy } = getCurrentInstance();
import { useRoute, useRouter } from "vue-router";
const route = useRoute();
const router = useRouter();

import { mitter } from "@/eventbus/eventBus.js";

const startUpload = ref(false);
mitter.on("startUpload", (fileName) => {
  startUpload.value = true;
  nextTick(() => {
    formDataRef.value.resetFields();
    formData.value = {};
    formData.value.tags = [];
    formData.value.videoTitle = fileName;
  });
});

const formData = ref({
  tags: [],
  disableDanmaku: false,
  disableComment: false,
});
const formDataRef = ref();
const rules = {
  videoCover: [{ required: true, message: "Cover is required" }],
  videoTitle: [{ required: true, message: "Title is required" }],
  postType: [{ required: true, message: "Type is required" }],
  originInfo: [{ required: true, message: "Source information is required" }],
  categoryArray: [{ required: true, message: "Category is required" }],
  tags: [{ required: true, message: "Tags are required" }],
};

provide("cutImageCallback", ({ coverImage }) => {
  formData.value.videoCover = coverImage;
});

const videoUploaderRef = ref();
const videoList = ref([]);

const submitForm = () => {
  const uploadFileList = videoUploaderRef.value.getUploadFileList();
  if (!uploadFileList) {
    return;
  }

  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }

    let params = {
      ...formData.value,
      fileInfoList: uploadFileList,
    };

    // 处理分区
    params.parentCategoryId = params.categoryArray[0];
    if (params.categoryArray.length > 1) {
      params.categoryId = params.categoryArray[1];
    }
    delete params.categoryArray;

    // 处理标签
    if (params.tags instanceof Array) {
      params.tags = params.tags.join(",");
    }

    // 处理互动设置
    params.allowDanmaku = params.disableDanmaku ? 0 : 1;
    params.allowComment = params.disableComment ? 0 : 1;

    delete params.disableDanmaku;
    delete params.disableComment;

    // 处理封面
    if (params.videoCover instanceof File) {
      const videoCover = await uploadImage(params.videoCover);
      if (!videoCover) {
        return;
      }
      params.videoCover = videoCover;
    }

    params.introduction = params.introduction || "";

    let result = await proxy.Request({
      url: proxy.Api.postVideo,
      showLoading: true,
      params,
      dataType: "json",
    });

    if (!result) {
      return;
    }

    proxy.Message.success("Published successfully");
    router.push("/ucenter/video");
  });
};

//编辑
const videoId = ref();
const init = async () => {
  nextTick(() => {
    videoUploaderRef.value.initUploader(startUpload.value, []);
  });
  if (videoId.value) {
    let result = await proxy.Request({
      url: proxy.Api.getVideoByVideoId,
      params: {
        videoId: videoId.value,
      },
    });
    if (!result) {
      return;
    }
    formData.value = result.data;
    //处理tags
    formData.value.tags = formData.value.tags.split(",");
    //处理分类
    formData.value.categoryArray = [];
    if (formData.value.parentCategoryId) {
      formData.value.categoryArray.push(formData.value.parentCategoryId);
    }
    if (formData.value.categoryId) {
      formData.value.categoryArray.push(formData.value.categoryId);
    }
    //处理互动设置
    formData.value.disableDanmaku = formData.value.allowDanmaku === 0;
    formData.value.disableComment = formData.value.allowComment === 0;

    nextTick(() => {
      videoUploaderRef.value.initUploader(
        startUpload.value,
        result.data.videoInfoFileList
      );
    });
  }
};
watch(
  () => route.query.videoId,
  (newVal, oldVal) => {
    if (newVal) {
      startUpload.value = true;
    } else {
      startUpload.value = false;
    }
    videoId.value = newVal;
    init();
  },
  { immediate: true, deep: true }
);

//重新加载
const reload = () => {};
onMounted(() => {
  window.addEventListener("beforeunload", reload);
});

onUnmounted(() => {
  mitter.off("startUpload");

  window.removeEventListener("beforeunload", reload);
});
</script>

<style lang="scss" scoped>
.upload-video-panel {
  background: #fff;
  padding: 20px;
}
.video-form {
  padding-right: 120px;
}
</style>
