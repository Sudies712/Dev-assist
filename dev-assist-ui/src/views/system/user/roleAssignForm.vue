<script setup lang="ts">
import {ref} from "vue";

const props = withDefaults(
    defineProps<{ formInline: { roleIds: number[]; roleOptions: any[] } }>(),
    {
      formInline: () => ({roleIds: [], roleOptions: []})
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
    <el-form-item label="角色">
      <el-checkbox-group v-model="newFormInline.roleIds">
        <el-checkbox
            v-for="r in newFormInline.roleOptions"
            :key="r.id"
            :value="r.id"
            class="!w-32"
        >
          {{ r.roleName }}
        </el-checkbox>
      </el-checkbox-group>
    </el-form-item>
    <div class="text-xs text-gray-400 pl-[90px]">
      全量覆盖该用户的角色（后端按所选角色全量重新分配）
    </div>
  </el-form>
</template>
