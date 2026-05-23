<template>
  <Dialog
    :show="dialogConfig.show"
    :title="dialogConfig.title"
    :buttons="dialogConfig.buttons"
    width="1000px"
    @close="dialogConfig.show = false"
  >
    <div class="cut-image-panel">
      <div class="cropper-wrapper">
        <VueCropper
          ref="cropperRef"
          class="cropper"
          :class="{ dragover: isDragover }"
          :img="sourceImage"
          outputType="png"
          :autoCrop="true"
          :autoCropWidth="props.cutWidth"
          :autoCropHeight="Math.round(props.cutWidth * props.scale)"
          :fixed="true"
          :fixedNumber="[1, props.scale]"
          :centerBox="true"
          :full="false"
          :fixedBox="true"
          @realTime="prview"
          mode="100%"
          @dragover.prevent="isDragover = true"
          @dragleave.prevent="isDragover = false"
          @drop.prevent="handleDrop"
        />

        <div v-if="!sourceImage" class="empty-tip">
          Drag an image here, or select one on the right
        </div>
      </div>

      <div class="preview-panel">
        <div class="preview-image">
          <img :src="previewsImage" />
        </div>
        <el-upload
          :multiple="false"
          :show-file-list="false"
          :http-request="selectFile"
          :accept="proxy.imageAccept"
        >
          <el-button class="select-btn" type="primary">Select Image</el-button>
        </el-upload>
      </div>
    </div>
    <div class="info">
      Recommended image size: at least {{ props.cutWidth }} *
      {{ Math.round(props.cutWidth * props.scale) }}
    </div>
  </Dialog>
</template>

<script setup>
import "vue-cropper/dist/index.css";
import { VueCropper } from "vue-cropper";

import { ref, reactive, getCurrentInstance, nextTick, inject } from "vue";
const { proxy } = getCurrentInstance();
import { useRoute, useRouter } from "vue-router";
const route = useRoute();
const router = useRouter();

/**
 * 参数说明
  img: '', // 裁剪图片的地址 url 地址, base64, blob
  outputSize: 1, // 裁剪生成图片的质量
  outputType: 'jpeg', // 裁剪生成图片的格式 jpeg, png, webp
  info: true, // 裁剪框的大小信息
  canScale: false, // 图片是否允许滚轮缩放
  autoCrop: true, // 是否默认生成截图框
  autoCropWidth: 150, // 默认生成截图框宽度
  autoCropHeight: 150, // 默认生成截图框高度
  fixedBox: false, // 固定截图框大小 不允许改变
  fixed: false, // 是否开启截图框宽高固定比例，这个如果设置为true，截图框会是固定比例缩放的，如果设置为false，则截图框的狂宽高比例就不固定了
  fixedNumber: [1, 1], // 截图框的宽高比例 [ 宽度 , 高度 ]
  canMove: true, // 上传图片是否可以移动
  canMoveBox: true, // 截图框能否拖动
  original: false, // 上传图片按照原始比例渲染
  centerBox: true, // 截图框是否被限制在图片里面
  infoTrue: true, // true 为展示真实输出图片宽高 false 展示看到的截图框宽高
  full: true, // 是否输出原图比例的截图
  enlarge: '1', // 图片根据截图框输出比例倍数
  mode: 'contain' // 图片默认渲染方式 contain , cover, 100px, 100% auto
 */
const props = defineProps({
  cutWidth: {
    type: Number,
    default: 400,
  },
  //高宽比例
  scale: {
    type: Number,
    default: 0.5,
  },
});

const dialogConfig = ref({
  show: false,
  title: "Crop Image",
  buttons: [
    {
      type: "primary",
      text: "Apply",
      click: (e) => {
        cutImage();
      },
    },
  ],
});

const cropperRef = ref();
const previewsImage = ref();
const prview = (data) => {
  cropperRef.value.getCropData((data) => {
    previewsImage.value = data;
  });
};

const sourceImage = ref();
const loadFile = (file) => {
  if (!file) return;

  let img = new FileReader();
  img.readAsDataURL(file);
  img.onload = ({ target }) => {
    sourceImage.value = target.result;
  };
};

const selectFile = (file) => {
  loadFile(file.file);
};

const show = (file) => {
  dialogConfig.value.show = true;
  sourceImage.value = "";
  previewsImage.value = "";

  nextTick(() => {
    if (file) {
      loadFile(file);
    }
  });
};

defineExpose({
  show,
});

const cutImageCallback = inject("cutImageCallback");
//裁剪
const cutImage = () => {
  //截图的高宽
  const cropW = Math.round(cropperRef.value.cropW);
  const cropH = Math.round(cropperRef.value.cropH);
  if (cropW == 0 || cropH == 0) {
    proxy.Message.warning(`Please select an image`);
    return;
  }
  if (
    cropW < props.cutWidth ||
    cropH < Math.round(props.cutWidth * props.scale)
  ) {
    proxy.Message.warning(
      `Image size must be at least ${props.cutWidth} * ${Math.round(
        props.cutWidth * props.scale
      )}`
    );
    return;
  }
  cropperRef.value.getCropBlob((blob) => {
    const file = new File(
      [blob],
      "temp." + blob.type.substring(blob.type.indexOf("/") + 1),
      { type: blob.type }
    );
    dialogConfig.value.show = false;

    cutImageCallback({
      coverImage: file,
    });
  });
};

const isDragover = ref(false);

const handleDrop = (e) => {
  isDragover.value = false;

  const file = e.dataTransfer.files?.[0];
  if (!file) return;

  if (!file.type.startsWith("image/")) {
    proxy.Message.error("只能上传图片");
    return;
  }

  loadFile(file);
};
</script>

<style lang="scss" scoped>
.cut-image-panel {
  display: flex;

  .cropper-wrapper {
    position: relative;
    flex: 1;
    height: 500px;
  }

  .cropper {
    width: 100%;
    height: 500px;
    border: 1px dashed #ddd;

    &.dragover {
      border-color: #409eff;
      background: #ecf5ff;
    }
  }

  .empty-tip {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: #999;
    pointer-events: none;
    z-index: 2;
  }

  .preview-panel {
    width: 200px;
    margin-left: 20px;
    text-align: center;

    .preview-image {
      width: 100%;
      height: 200px;
      background: #f6f6f6;
      display: flex;
      align-items: center;
    }

    img {
      width: 100%;
    }
  }

  .select-btn {
    margin-top: 20px;
  }
}

.info {
  color: #6b6b6b;
}
</style>
