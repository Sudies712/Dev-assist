import dayjs from "dayjs";
import {h, onMounted, reactive, ref, toRaw} from "vue";
import {message} from "@/utils/message";
import {addDialog} from "@/components/ReDialog";
import type {PaginationProps} from "@pureadmin/table";
import editForm from "../form.vue";
import burndownChart from "../burndown.vue";
import {getProjectList} from "@/api/devassist/project";
import {changeSprintStatus, createSprint, deleteSprint, getSprintList, updateSprint} from "@/api/devassist/sprint";

const STATUS_MAP = {
    NOT_STARTED: "未开始",
    IN_PROGRESS: "进行中",
    COMPLETED: "已完成",
    ARCHIVED: "已归档"
};
const STATUS_TYPE: any = {
    NOT_STARTED: "info",
    IN_PROGRESS: "success",
    COMPLETED: "primary",
    ARCHIVED: ""
};

export function useSprint() {
    const form = reactive({projectId: "", status: "", page: 1, pageSize: 10});
    const formRef = ref();
    const editFormRef = ref();
    const dataList = ref([]);
    const loading = ref(true);
    const projectOptions = ref([]);
    const pagination = reactive<PaginationProps>({
        total: 0,
        pageSize: 10,
        currentPage: 1,
        background: true
    });

    const columns: TableColumnList = [
        {label: "ID", prop: "id", width: 70},
        {label: "迭代名称", prop: "name", minWidth: 150},
        {label: "迭代目标", prop: "goal", minWidth: 180, showOverflowTooltip: true},
        {
            label: "状态",
            prop: "status",
            width: 100,
            cellRenderer: ({row}) => (
                <el-tag effect="plain" type={STATUS_TYPE[row.status] || ""}>
                    {STATUS_MAP[row.status] || row.status}
                </el-tag>
            )
        },
        {
            label: "起止时间",
            minWidth: 200,
            formatter: ({startDate, endDate}) =>
                `${startDate || "-"} ~ ${endDate || "-"}`
        },
        {
            label: "创建时间",
            prop: "createTime",
            minWidth: 150,
            formatter: ({createTime}) => dayjs(createTime).format("YYYY-MM-DD HH:mm")
        },
        {label: "操作", fixed: "right", width: 260, slot: "operation"}
    ];

    async function onSearch() {
        loading.value = true;
        try {
            const data: any = await getSprintList(toRaw(form));
            dataList.value = data?.list || [];
            pagination.total = data?.total || 0;
            pagination.pageSize = data?.pageSize || 10;
            pagination.currentPage = data?.currentPage || 1;
        } finally {
            loading.value = false;
        }
    }

    const resetForm = el => {
        if (!el) return;
        el.resetFields();
        onSearch();
    };

    function openDialog(title = "新增", row?: any) {
        addDialog({
            title: `${title}迭代`,
            props: {
                formInline: {
                    id: row?.id,
                    projectId: row?.projectId ?? form.projectId ?? "",
                    name: row?.name ?? "",
                    goal: row?.goal ?? "",
                    startDate: row?.startDate ?? "",
                    endDate: row?.endDate ?? "",
                    projectOptions: projectOptions.value
                }
            },
            width: "46%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () => h(editForm, {ref: editFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                editFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    if (title === "新增") {
                        await createSprint(cur);
                        message("新增成功", {type: "success"});
                    } else {
                        await updateSprint(cur.id, cur);
                        message("修改成功", {type: "success"});
                    }
                    done();
                    onSearch();
                });
            }
        });
    }

    /** 上下文状态动作：开始/完成/归档，由调用方传目标状态。迭代为线性状态机，每态仅一个下一步 */
    async function handleAction(row: any, target: string) {
        await changeSprintStatus(row.id, target);
        message("操作成功", {type: "success"});
        onSearch();
    }

    function handleBurndown(row) {
        addDialog({
            title: `燃尽图「${row.name}」`,
            width: "60%",
            closeOnClickModal: true,
            contentRenderer: () => h(burndownChart, {sprintId: row.id}),
            beforeSure: done => done()
        });
    }

    async function handleDelete(row) {
        await deleteSprint(row.id);
        message("删除成功", {type: "success"});
        onSearch();
    }

    function handleSizeChange(val: number) {
        form.pageSize = val;
        onSearch();
    }

    function handleCurrentChange(val: number) {
        form.page = val;
        onSearch();
    }

    onMounted(async () => {
        const p: any = await getProjectList({pageSize: 100});
        projectOptions.value = (p?.list || []).map((x: any) => ({
            id: x.id,
            name: x.name
        }));
        onSearch();
    });

    return {
        form,
        formRef,
        loading,
        columns,
        dataList,
        pagination,
        projectOptions,
        onSearch,
        resetForm,
        openDialog,
        handleAction,
        handleBurndown,
        handleDelete,
        handleSizeChange,
        handleCurrentChange
    };
}
