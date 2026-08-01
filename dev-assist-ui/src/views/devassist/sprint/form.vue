<script setup lang="ts">
import {ref} from "vue";
import ReCol from "@/components/ReCol";

const props = withDefaults(
    defineProps<{ formInline: any }>(),
    {
      formInline: () => ({
        projectId: "",
        name: "",
        goal: "",
        startDate: "",
        endDate: "",
        projectOptions: []
      })
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  projectId: [{required: true, message: "请选择项目", trigger: "change"}],
  name: [{required: true, message: "请输入迭代名称", trigger: "blur"}]
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
        <el-form-item label="迭代名称" prop="name">
          <el-input v-model="newFormInline.name" clearable/>
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="迭代目标">
          <el-input
              v-model="newFormInline.goal"
              type="textarea"
              :rows="2"
              placeholder="本次迭代要达成的目标"
          />
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="开始日期">
          <el-date-picker
              v-model="newFormInline.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              class="w-full"
          />
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="结束日期">
          <el-date-picker
              v-model="newFormInline.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              class="w-full"
          />
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
