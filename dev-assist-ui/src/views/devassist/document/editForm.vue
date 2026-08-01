<script setup lang="ts">
import {ref} from "vue";
import ReCol from "@/components/ReCol";

const props = withDefaults(
    defineProps<{ formInline: any }>(),
    {
      formInline: () => ({
        id: undefined,
        type: "OTHER",
        description: ""
      })
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

function getRef() {
  return ruleFormRef.value;
}

defineExpose({getRef});
</script>

<template>
  <el-form ref="ruleFormRef" :model="newFormInline" label-width="90px">
    <el-row :gutter="30">
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="文档类型">
          <el-select v-model="newFormInline.type" class="w-full">
            <el-option label="需求" value="REQUIREMENT"/>
            <el-option label="设计" value="DESIGN"/>
            <el-option label="接口" value="API"/>
            <el-option label="测试" value="TEST"/>
            <el-option label="会议" value="MEETING"/>
            <el-option label="规范" value="STANDARD"/>
            <el-option label="迭代总结" value="SPRINT_SUMMARY"/>
            <el-option label="项目总结" value="PROJECT_SUMMARY"/>
            <el-option label="其他" value="OTHER"/>
          </el-select>
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="描述">
          <el-input
              v-model="newFormInline.description"
              type="textarea"
              :rows="2"
          />
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
