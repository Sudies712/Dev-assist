<script setup lang="ts">
import {onMounted, ref} from "vue";
import {listChunks} from "@/api/devassist/document";

const props = withDefaults(
    defineProps<{ formInline: { docId: number; docName: string; parseStatus: string } }>(),
    {
      formInline: () => ({docId: 0, docName: "", parseStatus: ""})
    }
);

const newFormInline = ref(props.formInline);
const chunks = ref<any[]>([]);
const loading = ref(false);

async function loadChunks() {
  loading.value = true;
  try {
    chunks.value = (await listChunks(newFormInline.value.docId)) || [];
  } finally {
    loading.value = false;
  }
}

onMounted(loadChunks);
</script>

<template>
  <div>
    <el-alert
        v-if="newFormInline.parseStatus !== 'PARSED'"
        :title="
        newFormInline.parseStatus === 'PARSING'
          ? '文档解析中，暂无切片'
          : newFormInline.parseStatus === 'FAILED'
            ? '文档解析失败，请重新解析'
            : '文档尚未解析'
      "
        type="warning"
        :closable="false"
        class="mb-3"
    />
    <el-timeline v-if="chunks.length">
      <el-timeline-item
          v-for="(c, i) in chunks"
          :key="c.id"
          :timestamp="`切片 ${c.chunkIndex ?? i + 1}`"
          placement="top"
      >
        <el-card shadow="never">
          <div class="whitespace-pre-wrap text-sm leading-6">{{ c.content }}</div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
    <el-empty
        v-else
        v-loading="loading"
        description="暂无切片"
    />
  </div>
</template>
