<script setup lang="ts">
import {ref} from "vue";
import ReCol from "@/components/ReCol";

const props = withDefaults(
    defineProps<{ formInline: any }>(),
    {
      formInline: () => ({
        id: undefined,
        projectId: "",
        sprintId: "",
        title: "",
        priority: "MEDIUM",
        preconditions: "",
        steps: "",
        expectedResult: "",
        projectOptions: [],
        sprintOptions: []
      })
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  projectId: [{required: true, message: "请选择项目", trigger: "change"}],
  title: [{required: true, message: "请输入用例标题", trigger: "blur"}]
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
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="所属项目" prop="projectId">
          <el-select
              v-model="newFormInline.projectId"
              placeholder="请选择项目"
              class="w-full"
              :disabled="!!newFormInline.id"
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
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="所属迭代">
          <el-select
              v-model="newFormInline.sprintId"
              placeholder="可不选"
              clearable
              class="w-full"
          >
            <el-option
                v-for="s in newFormInline.sprintOptions"
                :key="s.id"
                :label="s.name"
                :value="s.id"
            />
          </el-select>
        </el-form-item>
      </re-col>
      <re-col :value="16" :xs="24" :sm="24">
        <el-form-item label="用例标题" prop="title">
          <el-input v-model="newFormInline.title" clearable/>
        </el-form-item>
      </re-col>
      <re-col :value="8" :xs="24" :sm="24">
        <el-form-item label="优先级">
          <el-select v-model="newFormInline.priority" class="w-full">
            <el-option label="低" value="LOW"/>
            <el-option label="中" value="MEDIUM"/>
            <el-option label="高" value="HIGH"/>
          </el-select>
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="前置条件">
          <el-input
              v-model="newFormInline.preconditions"
              type="textarea"
              :rows="2"
              placeholder="执行本用例前需满足的条件"
          />
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="测试步骤">
          <el-input
              v-model="newFormInline.steps"
              type="textarea"
              :rows="3"
              placeholder="按步骤描述如何执行"
          />
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="预期结果">
          <el-input
              v-model="newFormInline.expectedResult"
              type="textarea"
              :rows="2"
          />
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
