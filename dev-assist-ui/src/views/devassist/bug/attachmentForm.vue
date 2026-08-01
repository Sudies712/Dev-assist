<script setup lang="ts">
import {onMounted, ref} from "vue";
import dayjs from "dayjs";
import {UploadFilled} from "@element-plus/icons-vue";
import {message} from "@/utils/message";
import type {UploadRequestOptions} from "element-plus";
import {downloadAttachment, listAttachments, uploadAttachment} from "@/api/devassist/bug";

const props = withDefaults(
    defineProps<{ formInline: { bugId: number; bugTitle: string } }>(),
    {
      formInline: () => ({bugId: 0, bugTitle: ""})
    }
);

const newFormInline = ref(props.formInline);
const attachments = ref<any[]>([]);
const uploading = ref(false);

async function loadAttachments() {
  attachments.value = (await listAttachments(newFormInline.value.bugId)) || [];
}

/** el-upload 自定义上传：走 http（带 token），成功后刷新列表 */
async function customUpload(options: UploadRequestOptions) {
  uploading.value = true;
  try {
    await uploadAttachment(newFormInline.value.bugId, options.file as File);
    message("上传成功", {type: "success"});
    await loadAttachments();
  } catch {
    // 错误已由 http 响应拦截器统一提示
  } finally {
    uploading.value = false;
  }
}

function formatSize(size: number) {
  if (!size && size !== 0) return "-";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(2)} MB`;
}

function handleDownload(row: any) {
  downloadAttachment(
      newFormInline.value.bugId,
      row.id,
      row.fileName || "attachment"
  );
}

onMounted(loadAttachments);
</script>

<template>
  <div>
    <el-upload
        drag
        multiple
        :show-file-list="false"
        :auto-upload="true"
        :http-request="customUpload"
        class="w-full"
    >
      <el-icon class="el-icon--upload" :class="{ 'is-loading': uploading }">
        <UploadFilled/>
      </el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持任意类型文件；仅提交人或项目负责人可上传。
        </div>
      </template>
    </el-upload>

    <el-table
        :data="attachments"
        size="small"
        class="mt-3"
        empty-text="暂无附件"
    >
      <el-table-column label="文件名" prop="fileName" min-width="180"/>
      <el-table-column label="大小" width="110">
        <template #default="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="上传人" prop="uploaderName" width="110"/>
      <el-table-column label="上传时间" width="160">
        <template #default="{ row }">
          {{ dayjs(row.uploadTime).format("YYYY-MM-DD HH:mm") }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button
              link
              type="primary"
              size="small"
              @click="handleDownload(row)"
          >
            下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
