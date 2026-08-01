<script setup lang="ts">
import {ref} from "vue";
import ReCol from "@/components/ReCol";

const props = withDefaults(
    defineProps<{
      formInline: {
        projectId: number | string;
        title: string;
        type: string;
        priority: string;
        estimatedEffort: number | string;
        description: string;
        projectOptions: Array<{ id: number; name: string }>;
      };
    }>(),
    {
      formInline: () => ({
        projectId: "",
        title: "",
        type: "FUNCTIONAL",
        priority: "MEDIUM",
        estimatedEffort: "",
        description: "",
        projectOptions: []
      })
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  projectId: [{required: true, message: "请选择所属项目", trigger: "change"}],
  title: [{required: true, message: "请输入需求标题", trigger: "blur"}]
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
    <el-row :gutter="30">
      <re-col :value="24">
        <el-form-item label="所属项目" prop="projectId">
          <el-select
              v-model="newFormInline.projectId"
              placeholder="请选择项目"
              class="w-full"
              :disabled="newFormInline.projectOptions.length === 0"
          >
            <el-option
                v-for="p in newFormInline.projectOptions"
                :key="p.id"
                :label="p.name"
                :value="p.id"
            />
          </el-select>
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="newFormInline.title" clearable/>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="类型">
          <el-select v-model="newFormInline.type" class="w-full">
            <el-option label="功能需求" value="FUNCTIONAL"/>
            <el-option label="非功能需求" value="NON_FUNCTIONAL"/>
            <el-option label="缺陷" value="DEFECT"/>
            <el-option label="优化" value="IMPROVEMENT"/>
          </el-select>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="优先级">
          <el-select v-model="newFormInline.priority" class="w-full">
            <el-option label="低" value="LOW"/>
            <el-option label="中" value="MEDIUM"/>
            <el-option label="高" value="HIGH"/>
            <el-option label="紧急" value="URGENT"/>
          </el-select>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="故事点">
          <el-input-number
              v-model="newFormInline.estimatedEffort"
              :min="0"
              controls-position="right"
              class="w-full!"
          />
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="需求描述">
          <el-input
              v-model="newFormInline.description"
              type="textarea"
              :rows="3"
          />
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
