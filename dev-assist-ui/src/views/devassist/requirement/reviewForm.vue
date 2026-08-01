<script setup lang="ts">
import {ref} from "vue";

const props = withDefaults(
    defineProps<{
      formInline: { result: string; opinion: string };
    }>(),
    {formInline: () => ({result: "PASS", opinion: ""})}
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  result: [{required: true, message: "请选择评审结果", trigger: "change"}]
};

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
      label-width="90px"
  >
    <el-form-item label="评审结果" prop="result">
      <el-radio-group v-model="newFormInline.result">
        <el-radio value="PASS">通过（确认）</el-radio>
        <el-radio value="REJECT">拒绝（关闭）</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="评审意见">
      <el-input
          v-model="newFormInline.opinion"
          type="textarea"
          :rows="4"
          placeholder="请输入评审意见"
      />
    </el-form-item>
  </el-form>
</template>
