import dayjs from "dayjs";
import {h, onMounted, reactive, ref, toRaw} from "vue";
import {message} from "@/utils/message";
import {addDialog} from "@/components/ReDialog";
import type {PaginationProps} from "@pureadmin/table";
import editForm from "../form.vue";
import {changeProjectStatus, createProject, getProjectList, updateProject} from "@/api/devassist/project";

const STATUS_MAP = {
    NOT_STARTED: "未开始",
    IN_PROGRESS: "进行中",
    PAUSED: "暂停中",
    COMPLETED: "已完成",
    ARCHIVED: "已归档"
};

const STATUS_TYPE: any = {
    NOT_STARTED: "info",
    IN_PROGRESS: "success",
    PAUSED: "warning",
    COMPLETED: "primary",
    ARCHIVED: ""
};

export function useProject() {
    const form = reactive({
        name: "",
        status: "",
        page: 1,
        pageSize: 10
    });
    const formRef = ref();
    const detailVisible = ref(false);
    const detailProjectId = ref<number | null>(null);
    const dataList = ref([]);
    const loading = ref(true);
    const pagination = reactive<PaginationProps>({
        total: 0,
        pageSize: 10,
        currentPage: 1,
        background: true
    });

    const columns: TableColumnList = [
        {label: "ID", prop: "id", width: 70},
        {label: "项目名称", prop: "name", minWidth: 150},
        {label: "技术栈", prop: "techStack", minWidth: 130},
        {
            label: "状态",
            prop: "status",
            minWidth: 100,
            cellRenderer: ({row}) => (
                <el-tag effect="plain" type={STATUS_TYPE[row.status] || ""}>
                    {STATUS_MAP[row.status] || row.status}
                </el-tag>
            )
        },
        {
            label: "开始时间",
            prop: "startDate",
            minWidth: 120,
            formatter: ({startDate}) => startDate || "-"
        },
        {
            label: "结束时间",
            prop: "endDate",
            minWidth: 120,
            formatter: ({endDate}) => endDate || "-"
        },
        {
            label: "创建时间",
            prop: "createTime",
            minWidth: 150,
            formatter: ({createTime}) =>
                dayjs(createTime).format("YYYY-MM-DD HH:mm")
        },
        {label: "操作", fixed: "right", width: 280, slot: "operation"}
    ];

    async function onSearch() {
        loading.value = true;
        try {
            // http 响应拦截器已脱信封 return data（{list,total,pageSize,currentPage}）
            const data: any = await getProjectList(toRaw(form));
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
        void onSearch();
    };

    function openDialog(title = "新增", row?: any) {
        addDialog({
            title: `${title}项目`,
            props: {
                formInline: {
                    id: row?.id,
                    name: row?.name ?? "",
                    description: row?.description ?? "",
                    techStack: row?.techStack ?? ""
                }
            },
            width: "46%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () => h(editForm, {ref: formRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const curData = options.props.formInline;
                const FormRef = formRef.value.getRef();
                FormRef.validate(async (valid: boolean) => {
                    if (!valid) return;
                    if (title === "新增") {
                        await createProject(curData);
                        message("新增成功", {type: "success"});
                    } else {
                        await updateProject(curData.id, curData);
                        message("修改成功", {type: "success"});
                    }
                    done();
                    void onSearch();
                });
            }
        });
    }

    /** 上下文状态动作：开始/暂停/继续/结束/归档，由调用方传目标状态 */
    async function handleAction(row: any, target: string) {
        await changeProjectStatus(row.id, target);
        message("操作成功", {type: "success"});
        void onSearch();
    }

    function handleSizeChange(val: number) {
        form.pageSize = val;
        void onSearch();
    }

    function handleCurrentChange(val: number) {
        form.page = val;
        void onSearch();
    }

    function openDetail(row: any) {
        detailProjectId.value = row.id;
        detailVisible.value = true;
    }

    onMounted(() => onSearch());

    return {
        form,
        formRef,
        loading,
        columns,
        dataList,
        pagination,
        onSearch,
        resetForm,
        openDialog,
        handleAction,
        handleSizeChange,
        handleCurrentChange,
        detailVisible,
        detailProjectId,
        openDetail
    };
}
