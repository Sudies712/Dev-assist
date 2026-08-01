<script setup lang="ts">
import {ref} from "vue";

const props = withDefaults(
    defineProps<{
      formInline: {
        sprintId: number | string;
        sprintOptions: Array<{ id: number; name: string }>;
      };
    }>(),
    {
      formInline: () => ({sprintId: "", sprintOptions: []})
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  sprintId: [{required: true, message: "请选择迭代", trigger: "change"}]
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
    <el-form-item label="目标迭代" prop="sprintId">
      <el-select
          v-model="newFormInline.sprintId"
          placeholder="请选择迭代"
          class="w-full"
          :disabled="newFormInline.sprintOptions.length === 0"
      >
        <el-option
            v-for="s in newFormInline.sprintOptions"
            :key="s.id"
            :label="s.name"
            :value="s.id"
        />
      </el-select>
      <div v-if="newFormInline.sprintOptions.length === 0" class="text-xs text-gray-400 mt-1">
        该需求所属项目暂无迭代，请先在迭代管理创建
      </div>
    </el-form-item>
  </el-form>
</template>
