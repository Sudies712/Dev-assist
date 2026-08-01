<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import {useDark, useECharts} from "@pureadmin/utils";
import {getSprintBurndown} from "@/api/devassist/sprint";

const props = defineProps({
  sprintId: {type: Number, required: true}
});

const {isDark} = useDark();
const theme = computed(() => (isDark.value ? "dark" : "light"));
const chartRef = ref();
const {setOptions} = useECharts(chartRef, {theme, renderer: "svg"});
const empty = ref(false);

onMounted(async () => {
  try {
    const data: any = await getSprintBurndown(props.sprintId);
    const list: any[] = Array.isArray(data) ? data : [];
    if (!list.length) {
      empty.value = true;
      return;
    }
    setOptions({
      tooltip: {trigger: "axis"},
      legend: {data: ["实际剩余", "理想线"], top: 0},
      grid: {left: 45, right: 20, top: 40, bottom: 35},
      xAxis: {
        type: "category",
        data: list.map(d => d.date),
        axisLabel: {fontSize: 10}
      },
      yAxis: {type: "value", name: "剩余任务数", minInterval: 1},
      series: [
        {
          name: "实际剩余",
          type: "line",
          data: list.map(d => d.remaining),
          smooth: true,
          areaStyle: {opacity: 0.1}
        },
        {
          name: "理想线",
          type: "line",
          data: list.map(d => d.ideal),
          lineStyle: {type: "dashed"},
          symbol: "none"
        }
      ]
    });
  } catch {
    empty.value = true;
  }
});
</script>

<template>
  <div
      v-if="empty"
      class="h-80 flex items-center justify-center text-gray-400"
  >
    暂无燃尽图数据（迭代需设置起止日期且有任务完成记录）
  </div>
  <div v-else ref="chartRef" style="width: 100%; height: 360px"/>
</template>
