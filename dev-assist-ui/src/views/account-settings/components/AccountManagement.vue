<script setup lang="ts">
import {reactive, ref} from "vue";
import {message} from "@/utils/message";
import {deviceDetection} from "@pureadmin/utils";
import {changeMyPassword} from "@/api/user";
import type {FormInstance, FormRules} from "element-plus";

defineOptions({
  name: "AccountManagement"
});

const list = ref([
  {
    key: "password",
    title: "账户密码",
    illustrate: "建议定期修改密码，保障账户安全",
    button: "修改"
  },
  {
    key: "phone",
    title: "密保手机",
    illustrate: "用于账户安全验证与消息通知",
    button: "修改"
  },
  {
    key: "email",
    title: "备用邮箱",
    illustrate: "可在「个人信息」中维护邮箱",
    button: "修改"
  }
]);

const pwdVisible = ref(false);
const pwdFormRef = ref<FormInstance>();
const pwdForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirm: ""
});
const pwdRules = reactive<FormRules>({
  oldPassword: [{required: true, message: "请输入原密码", trigger: "blur"}],
  newPassword: [
    {required: true, message: "请输入新密码", trigger: "blur"},
    {min: 6, message: "密码不少于 6 位", trigger: "blur"}
  ],
  confirm: [
    {required: true, message: "请再次输入新密码", trigger: "blur"},
    {
      validator: (_r, v, cb) =>
          v === pwdForm.newPassword ? cb() : cb(new Error("两次输入不一致")),
      trigger: "blur"
    }
  ]
});

function onClick(item) {
  if (item.key === "password") {
    pwdVisible.value = true;
    pwdForm.oldPassword = "";
    pwdForm.newPassword = "";
    pwdForm.confirm = "";
  } else if (item.key === "email") {
    message("请在「个人信息」页维护邮箱", {type: "info"});
  } else {
    message("该功能暂未开放", {type: "info"});
  }
}

const onSubmitPwd = async (formEl: FormInstance) => {
  await formEl.validate(async valid => {
    if (!valid) return;
    await changeMyPassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    });
    message("密码修改成功，请重新登录", {type: "success"});
    pwdVisible.value = false;
  });
};
</script>

<template>
  <div :class="['min-w-45', deviceDetection() ? 'max-w-full' : 'max-w-[70%]']">
    <h3 class="my-8!">账户管理</h3>
    <div v-for="(item, index) in list" :key="index">
      <div class="flex items-center">
        <div class="flex-1">
          <p>{{ item.title }}</p>
          <el-text class="mx-1" type="info">{{ item.illustrate }}</el-text>
        </div>
        <el-button type="primary" text @click="onClick(item)">
          {{ item.button }}
        </el-button>
      </div>
      <el-divider/>
    </div>

    <el-dialog
        v-model="pwdVisible"
        title="修改密码"
        width="420px"
        :close-on-modal="false"
    >
      <el-form
          ref="pwdFormRef"
          :model="pwdForm"
          :rules="pwdRules"
          label-width="90px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
              v-model="pwdForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入原密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
              v-model="pwdForm.newPassword"
              type="password"
              show-password
              placeholder="不少于 6 位"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input
              v-model="pwdForm.confirm"
              type="password"
              show-password
              placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button bg text @click="pwdVisible = false">取消</el-button>
        <el-button
            bg
            text
            type="primary"
            @click="onSubmitPwd(pwdFormRef)"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.el-divider--horizontal {
  border-top: 0.1px var(--el-border-color) var(--el-border-style);
}
</style>
