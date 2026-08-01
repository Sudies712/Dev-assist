<script setup lang="ts">
import {ref} from "vue";

const props = withDefaults(
    defineProps<{
      formInline: { assigneeId: number | string; memberOptions: any[] };
    }>(),
    {
      formInline: () => ({assigneeId: "", memberOptions: []})
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {};

function getRef() {
  return ruleFormRef.value;
}

defineExpose({getRef});
</script>

<template>
  <el-form ref="ruleFormRef" :model="newFormInline" label-width="90px">
    <el-form-item label="修复人">
      <el-select
          v-model="newFormInline.assigneeId"
          placeholder="选择成员（留空取消分配）"
          clearable
          class="w-full"
      >
        <el-option
            v-for="m in newFormInline.memberOptions"
            :key="m.userId"
            :label="`${m.realName}（${m.projectRole}）`"
            :value="m.userId"
        />
      </el-select>
    </el-form-item>
    <div class="text-xs text-gray-400 pl-[90px]">
      仅项目负责人可分配缺陷修复人
    </div>
  </el-form>
</template>
