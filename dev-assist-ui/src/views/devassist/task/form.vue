<script setup lang="ts">
import {ref} from "vue";
import ReCol from "@/components/ReCol";

const props = withDefaults(
    defineProps<{ formInline: any }>(),
    {
      formInline: () => ({
        projectId: "",
        sprintId: "",
        title: "",
        priority: "MEDIUM",
        description: "",
        estimatedHours: "",
        projectOptions: [],
        sprintOptions: []
      })
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  projectId: [{required: true, message: "请选择项目", trigger: "change"}],
  sprintId: [{required: true, message: "请选择迭代", trigger: "change"}],
  title: [{required: true, message: "请输入任务标题", trigger: "blur"}]
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
        <el-form-item label="所属迭代" prop="sprintId">
          <el-select
              v-model="newFormInline.sprintId"
              placeholder="请选择迭代"
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
      <re-col :value="24">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="newFormInline.title" clearable/>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="优先级">
          <el-select v-model="newFormInline.priority" class="w-full">
            <el-option label="低" value="LOW"/>
            <el-option label="中" value="MEDIUM"/>
            <el-option label="高" value="HIGH"/>
          </el-select>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="预估工时">
          <el-input-number
              v-model="newFormInline.estimatedHours"
              :min="0"
              :step="0.5"
              controls-position="right"
              class="w-full!"
          />
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="任务描述">
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
