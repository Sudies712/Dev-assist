<script setup lang="ts">
import {computed, ref} from "vue";
import ReCol from "@/components/ReCol";

const props = withDefaults(
    defineProps<{ formInline: any }>(),
    {
      formInline: () => ({
        id: undefined,
        username: "",
        password: "",
        realName: "",
        email: "",
        phone: "",
        roleIds: [],
        roleOptions: []
      })
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);
const isEdit = computed(() => !!newFormInline.value.id);

const rules = computed(() => {
  const r: any = {
    username: [{required: true, message: "请输入用户名", trigger: "blur"}],
    realName: [{required: true, message: "请输入姓名", trigger: "blur"}]
  };
  if (!isEdit.value) {
    r.password = [
      {required: true, message: "请输入初始密码", trigger: "blur"},
      {min: 6, max: 32, message: "密码长度 6-32 位", trigger: "blur"}
    ];
    r.roleIds = [
      {required: true, type: "array", min: 1, message: "请分配角色", trigger: "change"}
    ];
  }
  return r;
});

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
        <el-form-item label="用户名" prop="username">
          <el-input
              v-model="newFormInline.username"
              :disabled="isEdit"
              clearable
          />
        </el-form-item>
      </re-col>
      <re-col v-if="!isEdit" :value="12" :xs="24" :sm="24">
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="newFormInline.password" clearable show-password/>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="newFormInline.realName" clearable/>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="邮箱">
          <el-input v-model="newFormInline.email" clearable/>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="手机">
          <el-input v-model="newFormInline.phone" clearable/>
        </el-form-item>
      </re-col>
      <re-col v-if="!isEdit" :value="24">
        <el-form-item label="角色" prop="roleIds">
          <el-select
              v-model="newFormInline.roleIds"
              multiple
              placeholder="分配系统角色"
              class="w-full"
          >
            <el-option
                v-for="r in newFormInline.roleOptions"
                :key="r.id"
                :label="r.roleName"
                :value="r.id"
            />
          </el-select>
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
