<script setup lang="ts">
import {computed, ref, watch} from "vue";

const props = withDefaults(
    defineProps<{
      formInline: { result: string; actualResult: string; submitBug: boolean };
    }>(),
    {
      formInline: () => ({result: "", actualResult: "", submitBug: false})
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  result: [{required: true, message: "请选择执行结果", trigger: "change"}]
};

// 仅 FAILED 时才可联动建缺陷
const isFailed = computed(() => newFormInline.value.result === "FAILED");

// 非 FAILED 时重置 submitBug，避免误传
watch(
    () => newFormInline.value.result,
    r => {
      if (r !== "FAILED") newFormInline.value.submitBug = false;
    }
);

function getRef() {
  return ruleFormRef.value;
}

defineExpose({getRef});
</script>

<template>
  <el-form
      ref="ruleFormRef"
      :model="newFormInline"
      :rules="rules"
      label-width="100px"
  >
    <el-form-item label="执行结果" prop="result">
      <el-radio-group v-model="newFormInline.result">
        <el-radio-button value="PASSED">通过</el-radio-button>
        <el-radio-button value="FAILED">失败</el-radio-button>
        <el-radio-button value="BLOCKED">阻塞</el-radio-button>
        <el-radio-button value="SKIPPED">跳过</el-radio-button>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="实际结果">
      <el-input
          v-model="newFormInline.actualResult"
          type="textarea"
          :rows="3"
          placeholder="记录实际执行情况（失败时建议详填，将写入联动缺陷描述）"
      />
    </el-form-item>
    <el-form-item v-if="isFailed" label="联动建缺陷">
      <el-switch v-model="newFormInline.submitBug"/>
      <span class="ml-2 text-xs text-gray-400">
        开启后自动创建一条缺陷（标题「【用例失败】…」，关联本用例/需求/迭代）
      </span>
    </el-form-item>
    <div
        v-if="isFailed && !newFormInline.submitBug"
        class="text-xs text-gray-400 pl-[100px]"
    >
      提示：失败可一键联动建缺陷，便于跟踪修复
    </div>
  </el-form>
</template>
