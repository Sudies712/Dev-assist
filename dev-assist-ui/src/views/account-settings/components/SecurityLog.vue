<script setup lang="ts">
import {ref} from "vue";
import {deviceDetection} from "@pureadmin/utils";

defineOptions({
  name: "SecurityLog"
});

// 后端操作日志（operation_log）尚未接入写入，暂无数据可展示
const dataList = ref([]);
</script>

<template>
  <div :class="['min-w-45', deviceDetection() ? 'max-w-full' : 'max-w-[70%]']">
    <h3 class="my-8!">安全日志</h3>
    <el-empty description="暂无安全日志记录"/>
  </div>
</template>
