<script setup lang="ts">
import {ref} from "vue";
import ReCol from "@/components/ReCol";

type FormInline = {
  id?: number;
  name: string;
  description: string;
  techStack: string;
};

const props = withDefaults(defineProps<{ formInline: FormInline }>(), {
  formInline: () => ({
    name: "",
    description: "",
    techStack: ""
  })
});

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  name: [{required: true, message: "请输入项目名称", trigger: "blur"}]
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
        <el-form-item label="项目名称" prop="name">
          <el-input
              v-model="newFormInline.name"
              clearable
              placeholder="请输入项目名称"
          />
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="技术栈">
          <el-input
              v-model="newFormInline.techStack"
              clearable
              placeholder="如 Spring Boot + Vue"
          />
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="项目描述">
          <el-input
              v-model="newFormInline.description"
              type="textarea"
              :rows="3"
              placeholder="请输入项目描述"
          />
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
