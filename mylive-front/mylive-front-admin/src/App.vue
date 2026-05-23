
<template>
  <el-config-provider :locale="en" :message="config">
    <router-view></router-view>
  </el-config-provider>
</template>

<script setup>
import { ref, onBeforeMount } from 'vue'
import { ElConfigProvider } from 'element-plus'
import en from 'element-plus/es/locale/lang/en'

const config = ref({
  max: 1,
})
</script>

<style scoped>
</style>
