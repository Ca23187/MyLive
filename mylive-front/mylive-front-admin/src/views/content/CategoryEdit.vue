<template>
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" :showCancel="true"
    @close="dialogConfig.show = false">
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="130px" @submit.prevent>
      <el-form-item label="Category Code" prop="categoryCode">
        <el-input :maxLength="10" v-model="formData.categoryCode" :show-word-limit="true" :maxlength="30" />
      </el-form-item>
      <el-form-item label="Category Name" prop="categoryName">
        <el-input :maxLength="10" v-model="formData.categoryName" :show-word-limit="true" :maxlength="30" />
      </el-form-item>
      <template v-if="formData.parentCategoryId === 0">
        <el-form-item label="Icon" prop="icon">
          <ImageUpload v-model="formData.icon"></ImageUpload>
        </el-form-item>
        <el-form-item label="Background Image" prop="icon">
          <ImageUpload v-model="formData.background" :width="300" :height="150"></ImageUpload>
        </el-form-item>
      </template>
    </el-form>
  </Dialog>
</template>
<script setup>
import ImageUpload from '@/components/ImageUpload.vue'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
const { proxy } = getCurrentInstance()
import { uploadImage } from '@/utils/Api.js'
const dialogConfig = ref({
  show: false,
  title: 'Add Category',
  buttons: [
    {
      type: 'primary',
      text: 'Confirm',
      click: (e) => {
        submitForm()
      },
    },
  ],
})

const formData = ref({})
const formDataRef = ref()

const rules = {
  categoryCode: [{ required: true, message: 'Please enter a category code' }],
  categoryName: [{ required: true, message: 'Please enter a category name' }],
}

const showEdit = (data) => {
  dialogConfig.value.show = true
  nextTick(() => {
    formDataRef.value.resetFields()
    if (data.categoryId == null) {
      dialogConfig.value.title = 'Add Category'
    } else {
      dialogConfig.value.title = 'Edit Category'
    }
    formData.value = Object.assign({}, data)
  })
}

defineExpose({
  showEdit,
})

const emit = defineEmits(['reload'])
const submitForm = async () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    let params = {}
    Object.assign(params, formData.value)

    //上传封面
    if (params.icon instanceof File) {
      params.icon = await uploadImage(params.icon)
    }
    //上传背景图
    if (params.background instanceof File) {
      params.background = await uploadImage(params.background)
    }

    delete params.children

    let result = await proxy.Request({
      url: proxy.Api.saveCategory,
      params,
    })
    if (!result) {
      return
    }
    dialogConfig.value.show = false
    proxy.Message.success('Saved successfully')
    emit('reload')
  })
}
</script>

<style lang="scss" scoped>
</style>
